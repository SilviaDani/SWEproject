import os
import json
import random
import dataset
import time

class Subject:
    def __init__(self, id, state, symptoms = 0):
        """
        id: int
        state: int (0 = healthy, 1 = infected, 2 = infectious, 3 = healed, 4 = isolated)
        symptoms: int (0 = no symptoms, 1 = developing symptoms, 2 = symptomatic)
        """
        self.id = id
        self.state = state
        self.symptoms = symptoms

def read_data(filename):
    with open(filename, 'r') as file:
        data = json.load(file)
    return data

def print_event(event):
    if event["type"] == "External":
        print("\033[33m{}\033[00m".format(event))
    elif event["type"] == "Symptoms":
        print("\033[93m{}\033[00m".format(event))
    elif event["type"] == "Test":
        print("\033[90m{}\033[00m".format(event))
    elif event["type"] == "Infectious":
        print("\033[91m{}\033[00m".format(event))
    elif event["type"] == "Internal":
        print("\033[35m{}\033[00m".format(event))
    elif event["type"] == "Enter_Isolation":
        print("\033[94m{}\033[00m".format(event))
    elif event["type"] == "Heal" or event["type"] == "Exit_Isolation":
        print("\033[92m{}\033[00m".format(event))
    else:
        print(event)


def infect_subject(data, subjects, subject_id, current_time):
    min_infectious_hours = 12
    max_infectious_hours = 48
    subjects[subject_id-1].state = 1
    symptomatic_threshold = 0.1
    if random.random() < symptomatic_threshold:
        subjects[subject_id-1].symptoms = 0 # asymptomatic
    else:
        subjects[subject_id-1].symptoms = 1
        min_develop_symptoms_in = 12
        max_develop_symptoms_in = 48
        develop_symptoms_in = random.randint(min_develop_symptoms_in, max_develop_symptoms_in)
        event = dataset.create_event("Develop_Symptoms", [subject_id], current_time + develop_symptoms_in, risk_factor=None, result=None)
        data["events"].append(event)
    infectious_in = random.randint(min_infectious_hours, max_infectious_hours)
    event = dataset.create_event("Infectious", [subject_id], current_time + infectious_in, risk_factor=None, result=None)
    data["events"].append(event)
    data["events"].sort(key=lambda x: x["time"])
    print(f"\033[1mSubject {subject_id} is now infected\033[0m")
    return subjects

# DEPRECATED
def isolate_or_set_healing_subject(data, subjects, subject_id, current_time):
    min_healing_hours = 24 * 7
    max_healing_hours = 24 * 21
    min_isolate_in_hours = 1
    max_isolate_in_hours = 24 * 21
    symptomatic_threshold = 0.1
    # determine if subject is asymptomatic or not
    if random.random() > symptomatic_threshold:
        # subject is symptomatic
        subjects[subject_id-1].state = 4 # so it gets isolated
        isolate_in = random.randint(min_isolate_in_hours, max_isolate_in_hours)
        isolate_for = random.randint(isolate_in, max_isolate_in_hours)
        event = dataset.create_event("Enter_Isolation", [subject_id], current_time + isolate_in, risk_factor=None, result=None)
        data["events"].append(event)
        event = dataset.create_event("Exit_Isolation", [subject_id], current_time + isolate_for, risk_factor=None, result=None)
        data["events"].append(event)
        data["events"].sort(key=lambda x: x["time"])
    else:
        # subject is asymptomatic
        subjects[subject_id-1].state = 2
        heal_in = random.randint(min_healing_hours, max_healing_hours)
        event = dataset.create_event("Heal", [subject_id], current_time + heal_in, risk_factor=None, result=None)
        data["events"].append(event)
        data["events"].sort(key=lambda x: x["time"])
        print(f"\033[1mSubject {subject_id} is now infectious\033[0m")
    return subjects

def set_healing_time(data, subjects, subject_id, current_time):
    min_healing_hours = 24 * 7
    max_healing_hours = 24 * 21
    subjects[subject_id-1].state = 2
    heal_in = random.randint(min_healing_hours, max_healing_hours)
    event = dataset.create_event("Heal", [subject_id], current_time + heal_in, risk_factor=None, result=None)
    data["events"].append(event)
    data["events"].sort(key=lambda x: x["time"])
    print(f"\033[1mSubject {subject_id} is now infectious\033[0m")
    return subjects

def set_isolation_time(data, subjects, subject_id, current_time):
    min_isolate_in_hours = 1
    max_isolate_in_hours = 24 * 21
    isolate_in = random.randint(min_isolate_in_hours, max_isolate_in_hours)
    isolate_for = random.randint(isolate_in, max_isolate_in_hours)
    subjects[subject_id-1].state = 2 # so it gets isolated
    event = dataset.create_event("Enter_Isolation", [subject_id], current_time + isolate_in, risk_factor=None, result=None)
    data["events"].append(event)
    event = dataset.create_event("Exit_Isolation", [subject_id], current_time + isolate_for, risk_factor=None, result=None)
    data["events"].append(event)
    data["events"].sort(key=lambda x: x["time"])
    return subjects

def _export_data(data, filename):
    # There is a need to clean up the events from those that are not important, however, this can also be done before the numerical analysis, actually, it might be better.
    data["events"].sort(key=lambda x: x["time"])
    with open(filename, 'w') as file:
        json.dump(data, file, indent=4)
    print(f"Data exported to {filename}")



    

def simulate_one_iteration(data, n_subjects, export_data=False, exported_data_filename=None):
    # Create a list of subjects
    subjects = []
    for i in range(n_subjects):
        subjects.append(Subject(i, 0))
    if export_data:
        past_events = []
    while data["events"]:
        event = data["events"].pop(0)
        print_event(event)
        # print([subject.state for subject in subjects])
        # Get involved subjects
        involved_subjects = event["involved_subjects"]
        type = event["type"]
        current_time = event["time"]
        # Infection through external event
        if event["type"] == "External":
            if event["risk_factor"] > random.random():
                for subject in involved_subjects:
                    if subjects[subject - 1].state == 0:
                        subjects = infect_subject(data, subjects, subject, current_time) # subjects[subject - 1].state : 0 -> 1
        elif event["type"] == "Infectious":
            for subject in involved_subjects:
                print(subject)
                print("state", subjects[subject-1].state)
                if subjects[subject - 1].state == 1:
                    if subjects[subject - 1].symptoms == 0:
                        subjects = set_healing_time(data, subjects, subject, current_time) # subjects[subject - 1].state : 1 -> 3. Subject is asymptomatic
                    elif subjects[subject - 1].symptoms == 1 or subjects[subject - 1].symptoms == 2:
                        subjects = set_isolation_time(data, subjects, subject, current_time) # subjects[subject - 1].state : 1 -> 2 -> 4. Subject is symptomatic
        elif event["type"] == "Heal":
            for subject in involved_subjects:
                if subjects[subject - 1].state == 2:
                    subjects[subject - 1].state = 3
                    print(f"\033[1mSubject {subject} is now healed\033[0m")
        elif event["type"] == "Develop_Symptoms":
            for subject in involved_subjects:
                if subjects[subject - 1].symptoms == 1:
                    subjects[subject - 1].symptoms = 2
                    print(f"\033[1mSubject {subject} is now symptomatic\033[0m")
        elif event["type"] == "Enter_Isolation":
            for subject in involved_subjects:
                if subjects[subject - 1].state == 2:
                    subjects[subject - 1].state = 4
                    print(f"\033[1mSubject {subject} is now isolated\033[0m")
        elif event["type"] == "Exit_Isolation":
            for subject in involved_subjects:
                if subjects[subject - 1].state == 4:
                    subjects[subject - 1].state = 3
                    print(f"\033[1mSubject {subject} is now healthy\033[0m")
        elif event["type"] == "Internal":
            # Remove isolated and immune subjects from the event
            involved_subjects = [subject for subject in involved_subjects if subjects[subject - 1].state == 0 or subjects[subject - 1].state == 2] # only healthy and infectious subjects
            # Check if at least one subject is infectious
            infectious = False
            for subject in involved_subjects:
                if subjects[subject - 1].state == 2:
                    infectious = True
            if infectious:
                for subject in involved_subjects:
                    if event["risk_factor"] > random.random()  and subjects[subject - 1].state == 0:
                        subjects = infect_subject(data, subjects, subject, current_time) # subject[subject - 1].state : 0 -> 1
        elif event["type"] == "Symptoms":
            # Check if subject is symptomatic. if asymptomatic (0) -> symptoms = False, if they are developing symptoms (1) -> symptoms = random, if they is symptomatic (2) -> symptoms = True
            is_symptomatic = False
            for subject in involved_subjects:
                if subjects[subject - 1].symptoms == 2:
                    is_symptomatic = True
                    print(f"\033[1mSubject {subject} is symptomatic\033[0m")
                elif subjects[subject - 1].symptoms == 1:
                    is_symptomatic = random.random() < 0.5
                    print(f"\033[1mSubject {subject} is not symptomatic\033[0m")
                else:
                    print(f"\033[1mSubject {subject} is not symptomatic\033[0m")
            event["result"] = is_symptomatic
        elif event["type"] == "Test":
            is_positive = False
            for subject in involved_subjects:
                if subjects[subject - 1].state == 2 or subjects[subject - 1].state == 4:
                    is_positive = True
                    print(f"\033[38;5;208m\033[3mSubject {subject} is positive\033[0m")
                else:
                    print(f"\033[38;5;30m\033[3mSubject {subject} is negative\033[0m")
            event["result"] = is_positive
        if export_data:
            past_events.append(event)

    if export_data:
        _export_data({"events" : past_events, "n_subjects" : data["n_subjects"], "time_limit" : data["time_limit"], "n_contacts" : data["n_contacts"]}, f"{exported_data_filename}.json")     


if __name__=="__main__":
    print("Running simulation")

    # Read dataset
    data_filename = "dataset_s15_t28_60.json"
    data = read_data(data_filename)
    n_subjects = data["n_subjects"]
    timelimit = data["time_limit"]

    # Sort dataset by timestamp
    data["events"].sort(key=lambda x: x["time"])

    # Print dataset
    for event in data["events"]:
        print(event)

    # run 1 iteration of the simulation
    tic = time.time()
    simulate_one_iteration(data, n_subjects, True, data_filename.split(".")[0] + "_simulated")
    toc = time.time()
    print(f"Simulation took {toc - tic} seconds")

