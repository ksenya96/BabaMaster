/*import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;*/

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GAExample extends Heuristics {
    public static void main(String[] args) {
        int n = 5;
        double[][] c = new double[n][n];

        Random random = new Random();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                c[i][j] = random.nextDouble() + 1;
            }
        }
        //getOptimalSolution(c, n);
        int numberOfSolution = 100;
        List<List<Integer>> solutions = new ArrayList<>(numberOfSolution);
        for (int i = 0; i < numberOfSolution; i++) {
            solutions.add(new ArrayList<>());
            for (int j = 0; j < n; j++) {
                solutions.get(i).add(j);
            }
            Collections.shuffle(solutions.get(i));
        }

        double bestCost = -1;
        int bestIndex = -1;
        List<Integer> bestSolution;

        //evaluate
        for (int i = 0; i < solutions.size(); i++) {
            double cost = getFitnessFunctionValue(c, solutions.get(i));
            if (cost > bestCost) {
                bestCost = cost;
                bestIndex = i;
                System.out.println("Best cost " + bestCost + " iteration " + 1 + solutions.get(bestIndex));
            }
        }

        for (int cnt = 1; cnt < 100; cnt++) {
            bestSolution = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                bestSolution.add(0);
            }
            Collections.copy(bestSolution, solutions.get(bestIndex));
            //выбираем родителей
            for (List<Integer> solution: solutions) {
                int pr = random.nextInt(100);
                if (pr < 50) {
                    int index1 = random.nextInt(numberOfSolution);
                    int index2 = random.nextInt(numberOfSolution);
                    while (index1 == index2) {
                        index2 = random.nextInt(numberOfSolution);
                    }
                    //скрещивание
                    List<Integer> child1 = crossover(solutions.get(index2));
                    List<Integer> child2 = crossover(solutions.get(index1));
                    Collections.copy(solutions.get(index1), child1);
                    Collections.copy(solutions.get(index2), child2);
                }
            }

            //поставить куда-нибудь лучшее решение
            int index = random.nextInt(numberOfSolution);
            while (index == bestIndex) {
                index = random.nextInt(numberOfSolution);
            }
            Collections.copy(solutions.get(index), bestSolution);

            //evaluate
            for (int i = 0; i < solutions.size(); i++) {
                double cost = getFitnessFunctionValue(c, solutions.get(i));
                if (cost > bestCost) {
                    bestCost = cost;
                    bestIndex = i;
                    System.out.println("Best cost " + bestCost + " iteration " + cnt + solutions.get(bestIndex));
                }
            }
            //отбор
            selection(solutions, c);
        }
        //System.out.println(solutions.stream().mapToDouble((solution) -> getFitnessFunctionValue(c, solution)).max());
    }

    private static void selection(List<List<Integer>> solutions, double[][] c) {
        int numberOfSolutions = solutions.size();
        Random random = new Random();
        for (int i = 0; i < numberOfSolutions; i++) {
            int pr = random.nextInt(100);
            if (pr < 20) {
                int index1 = random.nextInt(numberOfSolutions);
                int index2 = random.nextInt(numberOfSolutions);
                while (index1 == index2) {
                    index2 = random.nextInt(numberOfSolutions);
                }
                double cost1 = getFitnessFunctionValue(c, solutions.get(index1));
                double cost2 = getFitnessFunctionValue(c, solutions.get(index2));
                if (cost1 > cost2) {
                    Collections.copy(solutions.get(index2), solutions.get(index1));
                } else {
                    Collections.copy(solutions.get(index1), solutions.get(index2));
                }
            }
        }
    }

    /*private static void getOptimalSolution(double[][] c, int n) {
        double[] doubles = new double[n * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                doubles[i * n + j] = c[i][j];
            }
        }
        try {
            IloCplex cplex = new IloCplex();
            IloIntVar[] x = cplex.boolVarArray(n * n);
            cplex.addMaximize(cplex.scalProd(x, doubles));
            for (int i = 0; i < n; i++) {
                IloLinearIntExpr constr1 = cplex.linearIntExpr();
                IloLinearIntExpr constr2 = cplex.linearIntExpr();
                for (int j = 0; j < n; j++) {
                    constr1.addTerm(x[i * n + j], 1);
                    constr2.addTerm(x[j * n + i], 1);
                }
                cplex.addEq(1, constr1);
                cplex.addEq(1, constr2);

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
                }
            }

        } catch (IloException e) {
            System.out.println("Cplex error");
        }
    }*/

    private static List<Integer> crossover(List<Integer> parent) {
        Random random = new Random();
        int n = parent.size();
        int index1 = random.nextInt(n);
        int index2 = random.nextInt(n);
        int diff = Math.abs(index1 - index2);
        while (index1 == index2 || diff + 1 >= n) {
            index2 = random.nextInt(n);
            diff = Math.abs(index1 - index2);
        }
        if (index2 < index1) {
            int w = index1;
            index1 = index2;
            index2 = w;
        }
        List<Integer> child = new ArrayList<>(n);

        for (int i = index2 + 1; i < n; i++) {
            child.add(parent.get(i));
        }
        for (int i = 0; i < index1; i++) {
            child.add(parent.get(i));
        }
        for (int i = index1; i <= index2; i++) {
            child.add(parent.get(i));
        }

        return child;
    }

    private static double getFitnessFunctionValue(double[][] c, List<Integer> solution) {
        double result = 0;
        for (int i = 0; i < solution.size(); i++) {
            result += c[i][solution.get(i)];
        }
        return result;
    }

}
