import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Det {

    static final int FIRST_SHIFT_START_TIME = 8;
    static final int FIRST_SHIFT_END_TIME = 12;
    static final int SECOND_SHIFT_START_TIME = 13;
    static final int SECOND_SHIFT_END_TIME = 17;
    static final int WORKING_HOURS = 9;

    static int shifts = 2;
    static int weeks = 1;
    static int days = 5;
    static int rooms = 10;
    static int delta1 = 30; //порог увеличения для интервалов первой смены
    static int delta2 = 90; //второй смены
    static int C = 0; //порог стоимости увеличения

    static int[] setSlotBounds(int T, boolean isBeginOfWorkDay) {
        int[] bounds = new int[T];
        int firstShiftTimeMoment = isBeginOfWorkDay ? FIRST_SHIFT_START_TIME : FIRST_SHIFT_END_TIME;
        int secondShiftTimeMoment = isBeginOfWorkDay ? SECOND_SHIFT_START_TIME : SECOND_SHIFT_END_TIME;
        for (int i = 0; i < weeks; i++) {
            for (int j = 0; j < days; j++) {
                for (int k = 0; k < rooms; k++) {
                    //первая смена
                    bounds[days * rooms * i + rooms * j + k] =
                            7 * 24 * 60 * i + 24 * 60 * j + firstShiftTimeMoment * 60;
                    //вторая смена
                    bounds[weeks * days * rooms + days * rooms * i + rooms * j + k] =
                            7 * 24 * 60 * i + 24 * 60 * j + secondShiftTimeMoment * 60;
                }
            }
        }
        return bounds;
    }

    static int[] setCostPerUnitTimeOfExtension(int T) {
        int[] c = new int[T];
        Arrays.fill(c, 1);
        return c;
    }

    static int[] setNumberOfPatientsInGroups(int G) {
        int[] n = new int[G];
        for (int i = 0; i < G; i++) {
            n[i] = 15; //rooms * 5;//random.nextInt(rooms) + 1;
        }
        return n;
    }

    static int getTotalNumberOfPatients(int[] n) {
        int m = 0;
        for (int i = 0; i < n.length; i++) {
            m += n[i];
        }
        return m;
    }

    static int[] setReadyDates(int G, int[] A) {
        int[] r = new int[G];
        for (int i = 0; i < G; i++) {
            r[i] = A[rooms * i];
        }
        return r;
    }

    static int[] setDueDates(int[] r) {
        int G = r.length;
        int[] d = new int[G];
        for (int i = 0; i < G; i++) {
            d[i] = r[i] + WORKING_HOURS * 60; // 9 часов рабочий день
        }
        return d;
    }

    static double[] setOperationsWeights(int[] n) {
        int G = n.length;
        double[] w = new double[G];
        w[G - 1] = 1;
        for (int i = G - 2; i >= 0; i--) {
            if (i == G - 2) {
                w[i] = w[i + 1] * n[i + 1] + 1;
            } else {
                w[i] = w[i + 1] + w[i + 1] * n[i + 1];
            }
        }
        return w;
    }

    static int[] setOperationsTimes(int m) {
        int[] p = new int[m];
        Random random = new Random();
        for (int i = 0; i < m; i++) {
            /*p[i] = random.nextInt(2);
            if (p[i] == 0)
                p[i] = 20;
            else
                p[i] = 100;*/
            p[i] = random.nextInt(180 + 11) + 10;
        }
        return p;
    }

    static int[] getGroupsForPatients(int m, int[] n) {
        int[] patientsAndGroups = new int[m];
        int k = 0;
        for (int i = 0; i < n.length; i++) {
            for (int j = 0; j < n[i]; j++) {
                patientsAndGroups[k] = i;
                k++;
            }
        }
        return patientsAndGroups;
    }

    static int getGroupByIndex(int index, int[] patientsAndGroups) {
        return patientsAndGroups[index];
    }

    static int[] p;
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

        p = setOperationsTimes(m);

        try {
            IloCplex cplex = new IloCplex();
            IloIntVar[] x = setX(T, m, cplex);
            System.out.println("X are set");
            IloNumVar[] z = setZ(T, cplex);
            System.out.println("Z are set");

            //целевая функция
            setObjectiveFunction(T, m, patientsAndGroups, w, cplex, x);
            System.out.println("Objective function is set");

            ArrayList<IloRange> ranges = new ArrayList<>();
            //ограничения
            //суммарная стоимость увеличения не должна превысить порог C
            setCostConstraint(c, z, cplex, ranges);
            System.out.println("Constr1 is set");

            //сумма x[j][t] для каждой операции j не должна быть больше 1
            setConstraintsForX(T, m, cplex, x, ranges);
            System.out.println("Constr2 are set");

            //x[j][t] = 0, если t-й времменной интервал недопустим для операции j
            setXEqZeroConstraints(A, B, patientsAndGroups, r, d, p, cplex, x, ranges);
            System.out.println("Constr3 are set");

            setMainConstraints(A, B, patientsAndGroups, r, d, p, cplex, x, z, ranges);
            System.out.println("Constr4 are set");

            System.out.println("Ready to solve");
            if (cplex.solve()) {
                cplex.output().println(cplex.getStatus());
                cplex.output().println(cplex.getObjValue());
                /*double[] resX = cplex.getValues(x);
                int zu = 0;
                for (int i = 0; i < m; i++) {
                    for (int j = 0; j < T; j++) {
                        if ((int) resX[i * T + j] == 1) {
                            zu++;
                        }
                        System.out.print((int)resX[i * T + j]);
                        System.out.println("Variable x[" + (i + 1) + "][" + (j + 1) + "]: Value = " + resX[i * T + j]);
                    }
                    //System.out.println();
                }
                System.out.println("количество назначенных пациентов " + zu);
                System.out.println(cplex.getMIPRelativeGap());*/

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
                }/*

                /*double[] slacks = cplex.getSlacks(ranges.toArray(new IloRange[ranges.size()]));
                for (int i = 0; i < slacks.length; i++)
                    System.out.println(slacks[i]);
                */

            }
            cplex.exportModel("mipex1.lp");
            cplex.end();
        } catch (IloException e) {
            System.err.println("Concert exception caught " + e);
        }
    }

    static void setMainConstraints(int[] A, int[] B, int[] patientsAndGroups, int[] r, int[] d, int[] p, IloCplex cplex, IloIntVar[] x, IloNumVar[] z, ArrayList<IloRange> ranges) throws IloException {
        int T = A.length;
        int m = p.length;
        for (int i = 0; i < T; i++) {
            for (int k = 0; k < m; k++) {
                if (r[getGroupByIndex(k, patientsAndGroups)] + p[k] <= B[i] &&
                    d[getGroupByIndex(k, patientsAndGroups)] - p[k] >= A[i]) {
                    IloLinearIntExpr duration = cplex.linearIntExpr();
                    for (int l = k; l < m; l++) {
                        duration.addTerm(p[l], x[l * T + i]);

                        //if (B[i] != d[getGroupByIndex(l, patientsAndGroups)])
                        ranges.add(cplex.addLe(
                                cplex.diff(
                                        cplex.sum(Math.max(A[i], r[getGroupByIndex(k, patientsAndGroups)]), duration),
                                        cplex.sum(Math.min(B[i], d[getGroupByIndex(l, patientsAndGroups)]),
                                                z[B[i] <= d[getGroupByIndex(l, patientsAndGroups)] ? i :
                                                        weeks * days * rooms + rooms * getGroupByIndex(l, patientsAndGroups)])
                                ),
                                0));
                        /*else
                            ranges.add(cplex.addEq(z[i], 0));*/

                        /*ranges.add(cplex.addLe(
                                cplex.sum(Math.max(A[i], r[getGroupByIndex(k, patientsAndGroups)]), duration),
                                d[getGroupByIndex(l, patientsAndGroups)]));*/
                    }
                }
            }
        }
    }

    static void setXEqZeroConstraints(int[] A, int[] B, int[] patientsAndGroups, int[] r, int[] d, int[] p, IloCplex cplex, IloIntVar[] x, ArrayList<IloRange> ranges) throws IloException {
        int m = p.length;
        int T = A.length;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < T; j++) {
                if (r[getGroupByIndex(i, patientsAndGroups)] + p[i] > B[j] || d[getGroupByIndex(i, patientsAndGroups)] - p[i] < A[j]) {
                    ranges.add(cplex.addEq(x[i * T + j], 0));
                }
            }
        }
    }

    static void setConstraintsForX(int T, int m, IloCplex cplex, IloIntVar[] x, ArrayList<IloRange> ranges) throws IloException {
        IloLinearIntExpr constr2;
        for (int i = 0; i < m; i++) {
            constr2 = cplex.linearIntExpr();
            for (int j = 0; j < T; j++) {
                constr2.addTerm(1, x[i * T + j]);
            }
            ranges.add(cplex.addLe(constr2, 1));
        }
    }

    static void setCostConstraint(int[] c, IloNumVar[] z, IloCplex cplex, ArrayList<IloRange> ranges) throws IloException {
        IloNumExpr constr = cplex.scalProd(c, z);
        ranges.add(cplex.addLe(constr, C));
    }

    static void setObjectiveFunction(int T, int m, int[] patientsAndGroups, double[] w, IloCplex cplex, IloNumVar[] x)
            throws IloException {
        IloLinearNumExpr objectiveFunction = cplex.linearNumExpr();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < T; j++) {
                objectiveFunction.addTerm(w[getGroupByIndex(i, patientsAndGroups)], x[i * T + j]);
            }
        }
        cplex.addMaximize(objectiveFunction);
    }

    static IloNumVar[] setZ(int T, IloCplex cplex) throws IloException {
        //переменные z
        //нижняя граница 0
        double[] zLowerBounds = new double[T];
        Arrays.fill(zLowerBounds, 0);
        //верхние границы delta1, delta2
        double[] zUpperBounds = new double[T];
        Arrays.fill(zUpperBounds, 0, T / 2, delta1);
        Arrays.fill(zUpperBounds, T / 2, T, delta2);
        //все переменные действительные числа
        IloNumVarType[] zTypes = new IloNumVarType[T];
        Arrays.fill(zTypes, IloNumVarType.Float);
        String[] names = new String[T];
        for (int i = 0; i < T; i++) {
            names[i] = "z(" + (i + 1) + ")";
        }
        return cplex.numVarArray(T, zLowerBounds, zUpperBounds, zTypes, names);
    }

    static IloIntVar[] setX(int T, int m, IloCplex cplex) throws IloException {
        //переменные x
        //нижняя граница 0
        //верхняя граница 1
        //все переменные целочисленные (булевы)
        String[] names = new String[m * T];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < T; j++) {
                names[i * T + j] = "x(" + (i + 1) + ")(" + (j + 1) + ")";
            }
        }
        return cplex.boolVarArray(m * T, names);
    }
}
