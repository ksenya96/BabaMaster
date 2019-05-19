package linearAssignment;

import java.util.HashSet;
import java.util.Random;

public class CostMatrix
{
    private int[] _agents;
    private int[][] _costArray;
    private int _n;

    public CostMatrix(int agents)
    {
        _agents = new int[agents];
        _costArray = new int[agents][agents];
        _n = agents;

        for (int i = 0; i < agents; ++i)
        {
            for (int j = 0; j < agents; ++j)
            {
                SetCost(i, j, 0);
            }
        }
    }

    public int[][] get_costArray() {
        return _costArray;
    }

    public CostMatrix(int agents, int cost)
    {
        _agents = new int[agents];
        _costArray = new int[agents][agents];
        _n = agents;

        for (int i = 0; i < agents; ++i)
        {
            for (int j = 0; j < agents; ++j)
            {
                int value = cost * (j + 1);
                SetCost(i, j, value);
            }

            cost += 1;
        }
    }

    // Generate cost matrix of arbitrary costs
    public CostMatrix(int agents, int tasks, Random rnd)
    {
        _agents = new int[agents];
        _costArray = new int[agents][agents];
        _n = agents;

        for (int i = 0; i < agents; ++i)
        {
            for (int j = 0; j < tasks; ++j)
            {
                SetCost(i, j, rnd.nextInt(90) + 10);
            }
        }
    }

    public void SetCost(int agent, int task, int cost)
    {
        if (agent < _n && task < _n)
        {
            _costArray[agent][task] = cost;
        }
    }

    public int GetCost(int worker, int task)
    {
        return _costArray[worker][task];
    }

    public int GetChromosomeCost(Chromosome chromosome, boolean maximise)
    {
        var totalCost = 0;
        var assignments = new HashSet<Integer>();

        for (int worker = 0; worker < _n; ++worker)
        {
            var task = chromosome.GetTask(worker);
            assignments.add(task);
            totalCost += GetCost(worker, task);
        }

        // Penalise cost asccording to constraint violations
        var violations = _n - assignments.size();

        return maximise ? totalCost - violations * 100 :
                totalCost + violations * 100;
    }
}

