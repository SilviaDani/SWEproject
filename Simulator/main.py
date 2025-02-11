import contextlib
import dataset
import simulation
from tqdm import tqdm

if __name__=="__main__":
    print("Hey")
    timelimits = {14, 21, 28}
    subjects = {5, 10, 15}
    total_iterations = len(timelimits) * len(subjects) * 3
    with tqdm(total = total_iterations, desc="Progress") as pbar, contextlib.redirect_stdout(None):
        for timelimit in timelimits:
            for n_subjects in subjects:
                filenames = dataset.create_datasets(f"dataset_s{n_subjects}_t{timelimit}", [n_subjects], [timelimit])
                for filename in filenames:
                    data = simulation.read_data(f"{filename}")
                    simulation.simulate_one_iteration(data, n_subjects, True, f"{filename.split(".")[0]}_simulated")
                    pbar.update(1)
