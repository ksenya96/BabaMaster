package linearAssignment;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Random;

class Program
{
    public static void main(String[] args)
    {
        PrintWriter writerExact = null;
        PrintWriter writerGa = null;
        try {
            writerExact = new PrintWriter(new OutputStreamWriter(new FileOutputStream("exact.txt")));
            writerGa = new PrintWriter(new OutputStreamWriter(new FileOutputStream("ga.txt")));
        } catch (IOException e) {
        }
        for (int tasks = 5; tasks <= 100; tasks += 5) {
            var popSize = 1000;
            var rnd = new Random();

            // Do we seek to maximise or minimise?
            var maximise = false;

            var alg = new Algorithm(rnd, popSize, tasks, maximise);
            var matrix = new CostMatrix(tasks);

            for (int i = 0; i < tasks; i++) {
                for (int j = 0; j < tasks; j++) {
                    matrix.SetCost(i, j, rnd.nextInt(5) + 1);
                }
            }

        /*matrix.SetCost(0, 0, 11);
        matrix.SetCost(0, 1, 7);
        matrix.SetCost(0, 2, 10);
        matrix.SetCost(0, 3, 17);
        matrix.SetCost(0, 4, 10);

        matrix.SetCost(1, 0, 13);
        matrix.SetCost(1, 1, 21);
        matrix.SetCost(1, 2, 7);
        matrix.SetCost(1, 3, 11);
        matrix.SetCost(1, 4, 13);

        matrix.SetCost(2, 0, 13);
        matrix.SetCost(2, 1, 13);
        matrix.SetCost(2, 2, 15);
        matrix.SetCost(2, 3, 13);
        matrix.SetCost(2, 4, 14);

        matrix.SetCost(3, 0, 18);
        matrix.SetCost(3, 1, 10);
        matrix.SetCost(3, 2, 13);
        matrix.SetCost(3, 3, 16);
        matrix.SetCost(3, 4, 14);

        matrix.SetCost(4, 0, 12);
        matrix.SetCost(4, 1, 8);
        matrix.SetCost(4, 2, 16);
        matrix.SetCost(4, 3, 19);
        matrix.SetCost(4, 4, 10);*/

            alg.Run(rnd, matrix, tasks);

            writerGa.println(alg.get_population().get_bestChromosomeCost());
            System.out.println("Exact solution");
            writerExact.println(getOptimalSolution(matrix.get_costArray(), tasks));
        }
        writerGa.close();
        writerExact.close();

    }

    private static double getOptimalSolution(int[][] c, int n) {
        double[] doubles = new double[n * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                doubles[i * n + j] = c[i][j];
            }
        }
        try {
            IloCplex cplex = new IloCplex();
            IloIntVar[] x = cplex.boolVarArray(n * n);
            cplex.addMinimize(cplex.scalProd(x, doubles));
            for (int i = 0; i < n; i++) {
                IloLinearIntExpr constr1 = cplex.linearIntExpr();
                IloLinearIntExpr constr2 = cplex.linearIntExpr();
                for (int j = 0; j < n; j++) {
                    constr1.addTerm(x[i * n + j], 1);
                    constr2.addTerm(x[j * n + i], 1);
                }
                cplex.addEq(1, constr1);
                cplex.addEq(1, constr2);
            }

            if (cplex.solve()) {

                System.out.println(cplex.getStatus());
                System.out.println(cplex.getObjValue());
                double[] res = cplex.getValues(x);
                for (int k = 0; k < n; k++) {
                    for (int j = 0; j < n; j++) {
                        System.out.print((int) res[k * n + j]);
                    }
                    System.out.println();
                }
                for (int k = 0; k < n; k++) {
                    for (int j = 0; j < n; j++) {
                        System.out.print(c[k][j] + " ");
                    }
                    System.out.println();
                }
                cplex.exportModel("mipex1.lp");
                double result = cplex.getObjValue();
                cplex.end();
                return result;
            }


        } catch (IloException e) {
            System.out.println("Cplex error");
        }
        return 0;
    }
}
