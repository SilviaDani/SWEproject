from scipy.stats import kendalltau
def kendall_tau_distance(tau1, tau2):
    """
    Compute the Kendall tau distance between two rankings.
    """
    return stats.kendalltau(tau1, tau2).statistic

def spearman_coefficient(x,y):
    """
    Compute the Spearman coefficient
    """
    return stats.spearmanr(x,y).correlation