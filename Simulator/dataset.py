#import vari
import os
import json
import random

#hyper-parameters
file = "path/filename"
n_subjects = {5, 10, 15}
time_limit = {14, 21, 28}

#create file json of dataset
def create_empty_json_file(filename):
    # Check if the file already exists
    if os.path.exists(filename):
        print(f"File '{filename}' already exists.")
        user_input = input("Do you want to delete the existing file and create a new one? (Y/N): ").strip().lower()
        if user_input == 'y':
            os.remove(filename)
            print(f"File '{filename}' deleted.")
            # Create an empty JSON file
            with open(filename, 'w') as file:
                json.dump({"events" : []}, file)  # Dump an empty JSON object
            print(f"File '{filename}' created successfully.")
        else:
            print("Exiting without creating a new file.")
            exit(1)
    else:
        # Create an empty JSON file
        with open(filename, 'w') as file:
            json.dump({"events" : []}, file)  # Dump an empty JSON object
        print(f"File '{filename}' created successfully.")

#sample involved subjects for a single event
def sample_involved_subjects(N):
    lower_bound = 2
    upper_bound = (N // 2) - 1 
    if upper_bound < lower_bound: # Fix to avoid "ValueError: empty range in randrange(2, 2)" with 5 subjects
        upper_bound = lower_bound + 1
    n_involved_subjects = random.randint(lower_bound, upper_bound)
    return random.sample(range(1, N + 1), n_involved_subjects)

#sample events timestamps
def sample_timestamps(M, T):
    T_hours = T * 24
    sampled = random.sample(range(0, T_hours + 1), M)
    return sorted(sampled)

#sample risk factor of a single event
def sample_risk_factor():
    return random.uniform(0, 1)

#sample number of external contact of a subject
def sample_external_contacts():
    return random.randint(0, 4)

#sample number of symptomps of a subject
def sample_symptoms(T):
    return random.randint(0, 2*T//7)

#sample symptomps of a subject
def get_sampled_symptoms(n_symptoms):
    symptoms = get_symptoms() #già in formato evento
    if symptoms == None: # FIXME
        return None
    sampled_symptoms = random.sample(symptoms, n_symptoms)
    return sampled_symptoms

#run simulation to get symptoms
def get_symptoms():
    #TODO implementare
    pass

#sample number of tests of a subject
def sample_tests(T):
    return random.randint(0, 2*T//7)

#sample tests of a subject
def get_sampled_tests(n_tests):
    tests = get_tests() #già in formato evento
    if tests == None: # FIXME
        return None
    sampled_tests = random.sample(tests, n_tests)
    return sampled_tests

#run simulation to get tests
def get_tests():
    #TODO implementare
    pass

#create the right format of the event
def create_event(event_type, involved_subjects, t, risk_factor = None, result = None):
    event = {
        "type": event_type,
        "involved_subjects": involved_subjects,
        "time": t,
        "risk_factor": risk_factor,
        "result": result
    }
    return event

# #add event to dataset
# def add_to_dataset(filename, event):
#     # Convert the event to a compact JSON string
#     event_json = json.dumps(event)
    
#     # Write the event to the file on a new line
#     with open(filename, "a") as file:
#         file.write(event_json + "\n")

def create_datasets(file, n_subjects, time_limit):
    """
    Create datasets with the given parameters.
    
    Parameters:
    file (str): The name of the file to create. Without extension.

    Returns:
    filenames (list): A list of the filenames created.
    """
    created_files = []
    for T in time_limit:
        for N in n_subjects:
            n_contacts = {N, 2*N, 4*N}
            event_type = "Event"
            for M in n_contacts:
                filename = f"{file}_{M}.json" #aggiungi numeri per distinguere tutti i dataset - 20/12 fatto
                created_files.append(filename)
                #TODO vogliamo un dataset che si resetta ogni volta o li teniamo al variare di tutti i parametri? - 20/12 teniamo tutto
                # create_empty_json_file(filename)
                dataset_placeholder = {"events" : [], "n_subjects" : N, "time_limit" : T, "n_contacts" : M}
                timestamps = sample_timestamps(M, T)
                for t in timestamps:
                    involved_subjects = sample_involved_subjects(N)
                    risk_factor = sample_risk_factor()
                    internal_contact = create_event("Internal", involved_subjects, t, risk_factor) # FIXME add internal type
                    dataset_placeholder["events"].append(internal_contact)
                for subject in range (1, N + 1):
                    n_external_contacts = sample_external_contacts()
                    timestamps = sample_timestamps(n_external_contacts, T)
                    for t in timestamps:
                        risk_factor = sample_risk_factor()
                        external_contact = create_event("External", [subject], t, risk_factor) # FIXME add external type
                        dataset_placeholder["events"].append(external_contact)
                
                for subject in range(1, N + 1):
                    n_symptoms = sample_symptoms(T)
                    symptoms_checks_timestamps = sample_timestamps(n_symptoms, T)
                    for t in symptoms_checks_timestamps:
                        symptoms = create_event("Symptoms", [subject], t, None, "to be defined")
                        dataset_placeholder["events"].append(symptoms)

                for subject in range(1, N + 1):
                    n_tests = sample_tests(T)
                    test_timestamps = sample_timestamps(n_tests, T)
                    for t in test_timestamps:
                        test = create_event("Test", [subject], t, None, "to be defined")
                        dataset_placeholder["events"].append(test)
                    
                # for subject in range (1, N + 1):
                #     n_symptoms = sample_symptoms(T)
                #     print("Nsymptoms @", n_symptoms)
                #     sampled_symptoms = get_sampled_symptoms(n_symptoms)
                #     if sampled_symptoms == None:
                #         continue
                #     for symptom in sampled_symptoms:
                #         dataset_placeholder["events"].append(symptom)
                # #event_type = "Test"
                # for subject in range (1, N + 1):
                #     n_tests = sample_tests(T)
                #     print("Ntests @", n_tests)
                #     sampled_tests = get_sampled_tests(n_tests)
                #     if sampled_tests == None:
                #         continue
                #     for test in sampled_tests:
                #         dataset_placeholder["events"].append(test)
                # Write the dataset to the file
                with open(filename, 'w') as f:
                    json.dump(dataset_placeholder, f, indent=4)
                    print(f"Dataset '{filename}' created successfully.")
    return created_files
