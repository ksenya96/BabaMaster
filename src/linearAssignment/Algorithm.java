package linearAssignment;

import java.util.Random;

public class Algorithm
{
    private Population _population;

    public Population get_population() {
        return _population;
    }

    public Algorithm(Random rnd, int populationSize, int tasks, boolean maximise)
    {
        _population = new Population(rnd, populationSize, tasks, maximise);
    }

    public void Run(Random rnd, CostMatrix costMatrix, int tasks)
    {
        var iteration = 1;
        _population.Evaluate(costMatrix, iteration);

        while (iteration < 1000)
        {
            _population.StoreBestSolution(tasks);
            //_population.Mutate(rnd);
            _population.ApplyCrossover(rnd, tasks);

            _population.SeedBestSolution(rnd);
            _population.Evaluate(costMatrix, iteration);
            _population.Selection(rnd);

            iteration++;
        }
    }
}

