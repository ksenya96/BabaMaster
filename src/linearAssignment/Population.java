package linearAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Population
{
    private List<Chromosome> _chromosomes;
    private long _bestChromosomeCost;
    private int _bestChromosomeIndex;
    private Chromosome _bestChromosome;
    private boolean _maximise;

    public long get_bestChromosomeCost() {
        return _bestChromosomeCost;
    }

    public Population(Random rnd, int populationSize, int taskSize, boolean maximise)
    {
        _bestChromosomeCost = maximise ? -1 : 9999999999L;
        _bestChromosomeIndex = -1;
        _chromosomes = new ArrayList<Chromosome>(populationSize);
        _maximise = maximise;

        CreateArbitraryPopulation(rnd, populationSize, taskSize);
    }

    public void CreateArbitraryPopulation(Random rnd, int populationSize, int taskSize)
    {
        for(var i = 0; i < populationSize; ++i)
        {
            _chromosomes.add(new Chromosome(rnd, taskSize));
        }
    }

    public void Evaluate(CostMatrix costMatrix, int iteration)
    {
        for (var i = 0; i < _chromosomes.size(); ++i)
        {
            var cost = costMatrix.GetChromosomeCost(_chromosomes.get(i), _maximise);
            _chromosomes.get(i).SetCost(cost);

            if (IsBetter(cost, _bestChromosomeCost))
            {
                _bestChromosomeCost = cost;
                _bestChromosomeIndex = i;
                _chromosomes.get(_bestChromosomeIndex).Print(iteration);
            }
        }
    }

    public void ApplyCrossover(Random rnd, int taskSize)
    {
        var size = _chromosomes.size();

        for (var chromosome : _chromosomes)
        {
            var prob = rnd.nextInt(100);

            if (prob < 50)
            {
                var index1 = rnd.nextInt(size);
                var index2 = rnd.nextInt(size);

                while (index1 == index2)
                {
                    index2 = rnd.nextInt(size);
                }

                Crossover(index1, index2, rnd, taskSize);
            }
        }
    }

    public void Crossover(int parentIndex1, int parentIndex2, Random rnd, int taskSize)
    {
        var chr1 = _chromosomes.get(parentIndex1);
        var chr2 = _chromosomes.get(parentIndex2);

        var child1 = chr1.Crossover(chr2, rnd);
        var child2 = chr2.Crossover(chr1, rnd);

        _chromosomes.get(parentIndex1).Copy(child1);
        _chromosomes.get(parentIndex2).Copy(child2);
    }

    public void Mutate(Random rnd)
    {
        for(var chromosome : _chromosomes)
        {
            var prob = rnd.nextInt(100);

            if (prob < 5)
            {
                chromosome.Mutation(rnd);
            }
        }
    }

    public void StoreBestSolution(int taskSize)
    {
        _bestChromosome = new Chromosome(taskSize);
        _bestChromosome.Copy(_chromosomes.get(_bestChromosomeIndex));
    }

    public void SeedBestSolution(Random rnd)
    {
        var index = rnd.nextInt(_chromosomes.size());

        while (index == _bestChromosomeIndex)
        {
            index = rnd.nextInt(_chromosomes.size());
        }

        _chromosomes.get(index).Copy(_bestChromosome);
    }

    public void Selection(Random rnd)
    {
        var size = _chromosomes.size();

        for (var i = 0; i < size; ++i)
        {
            var prob = rnd.nextInt(100);

            if (prob < 20)
            {
                var index1 = rnd.nextInt(size);
                var index2 = rnd.nextInt(size);

                while (index1 == index2)
                {
                    index2 = rnd.nextInt(size);
                }

                var cost1 = _chromosomes.get(index1).Cost();
                var cost2 = _chromosomes.get(index2).Cost();

                if (IsBetter(cost1, cost2))
                {
                    _chromosomes.get(index2).Copy(_chromosomes.get(index1));
                }
                else
                {
                    _chromosomes.get(index1).Copy(_chromosomes.get(index2));
                }
            }
        }
    }

    private boolean IsBetter(long cost1, long cost2)
    {
        return _maximise ? cost1 > cost2 : cost1 < cost2;
    }
}
