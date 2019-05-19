package linearAssignment;

import java.util.Random;

public class Chromosome
{
    private int[] _workers;
    private int _cost;
    private int _size;

    public Chromosome(Random rnd, int workers)
    {
        _size = workers;
        _workers = new int[workers];
        Generate(rnd, workers);
    }

    public Chromosome(int workers)
    {
        _size = workers;
        _workers = new int[workers];
    }

    public void Print(int iteration)
    {
        System.out.println("Iteration = " + iteration);
        System.out.println("Total cost = " + _cost);
        for(var i = 0; i < _size; ++i)
        {
            System.out.println("Worker[" + i + "] -> Task[" + _workers[i] + "]");
        }
        System.out.println();
    }

    public Chromosome Crossover(Chromosome chr, Random rnd)
    {
        var child = new Chromosome(_size);

        int index1 = rnd.nextInt(_size);
        int index2 = rnd.nextInt(_size);

        var diff = Math.abs(index1 - index2);

        while (index1 == index2 || diff + 1 >= _size )
        {
            index2 = rnd.nextInt(_size);
            diff = Math.abs(index1 - index2);
        }

        if (index2 < index1)
        {
            int tmp = index1;
            index1 = index2;
            index2 = tmp;
        }

        for (int i = 0; i < index1; ++i)
        {
            var task = GetTask(i);
            child.Assign(i, task);
        }

        for (int i = index1; i <= index2; ++i)
        {
            var task = chr.GetTask(i);
            child.Assign(i, task);
        }

        for (int i = index2 + 1; i < _size; ++i)
        {
            var task = GetTask(i);
            child.Assign(i, task);
        }

        return child;
    }

    public void Mutation(Random rnd)
    {
        for (int i = 0; i < _size; ++i)
        {
            if (rnd.nextInt(100) < 33)
                Assign(i, GetRandomTask(rnd, _size));
        }
    }

    public void Copy(Chromosome chr)
    {
        _cost = chr._cost;
        _size = chr._size;

        for(var i = 0; i < _size; ++i)
        {
            _workers[i] = chr._workers[i];
        }
    }

    public int WorkerCost(int worker)
    {
        return  _workers[worker];
    }

    public int Cost()
    {
        return _cost;
    }

    public int Size()
    {
        return _size;
    }

    public void SetCost(int cost)
    {
        _cost = cost;
    }

    public void Assign(int worker, int task)
    {
        _workers[worker] = task;
    }

    public int GetTask(int worker)
    {
        return _workers[worker];
    }

    public int GetRandomTask(Random rnd, int taskRange)
    {
        return rnd.nextInt(taskRange);
    }

    public void Generate(Random rnd, int taskRange)
    {
        int count = 0;
        for (var worker : _workers)
        {
            Assign(count, rnd.nextInt(taskRange));
            count++;
        }
    }
}

