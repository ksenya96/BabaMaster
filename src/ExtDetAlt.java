import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.ArrayList;

/**
 * Created by acer on 23.06.2018.
 */
public class ExtDetAlt extends DetAlt {
    public static void main(String[] args) {
        int T = shifts * weeks * days * rooms; //кол-во временных интервалов (2 смены, 5 дней в неделю, 3 комнаты)
        System.out.println("Кол-во временных интервалов: T = " + T);

        //в качестве единиц измерения берем минуты
        int[] A = setSlotBounds(T, true); //начальные моменты интервалов
        int[] B = setSlotBounds(T, false); //конечные моменты интервалов

        int[] c = setCostPerUnitTimeOfExtension(T); //стоимость единицы времени увеличения
        int G = weeks * days; //кол-во групп

        int[] n = setNumberOfPatientsInGroups(G); //кол-во пациентов в группах
        int m = getTotalNumberOfPatients(n); //кол-во операций
        int[] patientsAndGroups = getGroupsForPatients(m, n);
        System.out.println("Кол-во операций m = " + m);

        int[] r = setReadyDates(G, A); //сроки готовности для группы операций
        int[] d = setDueDates(r); //конечные сроки

        double[] w = setOperationsWeights(n); //веса групп операций

        int[] p = setOperationsTimes(m);


        try {
            IloCplex cplex = new IloCplex();
            IloIntVar[] x = setX(T, m, cplex);
            IloNumVar[] y = setY(m, cplex);
            IloNumVar[] z = setZ(T, cplex);


            //целевая функция
            setObjectiveFunction(patientsAndGroups, w, cplex, x, y, A);

            ArrayList<IloRange> ranges = new ArrayList<>();
            //ограничения
            //суммарная стоимость увеличения не должна превысить порог C
            setCostConstraint(c, z, cplex, ranges);
            System.out.println("Constr1 is set");

            //сумма по x[j][t] - y[j] = 0 по всем j
            setConstraintsForXAndY(cplex, x, y, ranges);
            System.out.println("Constr2 are set");

            //x[j][t] = 0, если t-й времменной интервал недопустим для операции j
            setXEqZeroConstraints(A, B, patientsAndGroups, r, d, p, cplex, x, ranges);
            System.out.println("Constr3 are set");

            setConstraintsForX(A, B, cplex, x, ranges);
            System.out.println("Constr4 are set");

            /*int prev = 0;
            for (int i = 1; i < T; i++) {
                if (A[prev] == A[i] && B[prev] == B[i]) {
                    for (int j = 0; j < m; j++) {
                        if (r[getGroupByIndex(j, patientsAndGroups)] + p[j] <= B[i] &&
                                d[getGroupByIndex(j, patientsAndGroups)] - p[j] >= A[i]) {
                            ranges.add(cplex.addGe(cplex.diff(x[j * T + prev], x[j * T + i]), 0));
                        }
                    }
                }
                prev = i;
            }
            System.out.println("Constr4 are set");*/

            setMainConstraints(A, B, patientsAndGroups, r, d, p, cplex, x, z, ranges);
            System.out.println("Constr5 are set");


            if (cplex.solve()) {
                cplex.output().println(cplex.getStatus());
                cplex.output().println(cplex.getObjValue());
                double[] resX     = cplex.getValues(x);
                int zu = 0;
                for (int i = 0; i < m; i++) {
                    for (int j = 0; j < T; j++) {
                        if ((int)resX[i * T + j] == 1)
                            zu++;
                        //System.out.print((int)resX[i * T + j]);
                        //System.out.println("Variable x[" + (i + 1) + "][" + (j + 1) + "]: Value = " + resX[i * T + j]);
                    }
                    //System.out.println();
                }
                System.out.println("количество назначенных пациентов " + zu);

                /*double[] resZ = cplex.getValues(z);
                for (int i = 0; i < T; i++) {
                    System.out.println("z[" + i + "] = " + resZ[i]);
                }*/

                /*for (int j = 0; j < T; ++j) {
                    double duration = A[j];
                    for (int i = 0; i < m; i++) {
                        duration += p[i] * resX[i * T + j];
                    }
                    double res = duration > B[j] ? duration - B[j] : 0;
                    System.out.println("z[" + j + "] = " + res);
                }*/

                /*double[] slacks = cplex.getSlacks(ranges.toArray(new IloRange[ranges.size()]));
                for (int i = 0; i < slacks.length; i++)
                    System.out.println(slacks[i]);
                */

            }
            cplex.exportModel("mipex1.lp");
            cplex.end();
        }
        catch (IloException e) {
            System.err.println("Concert exception caught " + e);
        }
    }

    static void setObjectiveFunction(int[] patientsAndGroups, double[] w, IloCplex cplex, IloNumVar[] x, IloNumVar[] y, int[] A)
            throws IloException {
        int T = A.length;
        int m = y.length;
        int Amax = A[0];
        for (int i = 1; i < T; i++) {
            if (A[i] > Amax)
                Amax = A[i];
        }
        int M = m * Amax;
        IloLinearNumExpr part1 = cplex.linearNumExpr();
        for (int i = 0; i < m; i++) {
                part1.addTerm(M * w[getGroupByIndex(i, patientsAndGroups)], y[i]);
        }
        IloLinearNumExpr part2 = cplex.linearNumExpr();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < T; j++) {
                //part2.addTerm(M * w[getGroupByIndex(i, patientsAndGroups)] - A[j], x[i * T + j]);
                part2.addTerm(A[j], x[i * T + j]);
            }
        }
        IloNumExpr objectiveFunction = cplex.diff(part1, part2);
        cplex.addMaximize(objectiveFunction);
    }
}
