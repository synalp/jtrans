package test.plugins.phonetiseur;

import java.util.ArrayList;
import java.util.LinkedList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import plugins.phonetiseur.Utils;
import plugins.phonetiseur.*;

public class UtilsTest {

    public UtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of stringToArrayString method, of class Utils.
     */
    @Test
    public void testStringToArrayString() {
        System.out.println("Utils.stringToArrayString");
        String str = "abcd";
        String[] t = Utils.stringToArrayString(str);
        assertEquals(t[0], "a");
        assertEquals(t[1], "b");
        assertEquals(t[2], "c");
        assertEquals(t[3], "d");
    }

    /**
     * Test of arrayListStringToArrayString method, of class Utils.
     */
    @Test
    public void testArrayListStringToArrayString() {
        System.out.println("Utils.arrayListStringToArrayString");
        ArrayList<String> al = new ArrayList<String>();
        al.add("1");
        al.add("3");
        al.add("2");
        String[] t = Utils.arrayListStringToArrayString(al);
        assertEquals(t[0], "1");
        assertEquals(t[1], "3");
        assertEquals(t[2], "2");
    }

    /**
     * Test of convertirMatricesCumulsEnMatricesProbas method, of class Utils.
     */
    @Test
    public void testTonvertirMatricesCumulsEnMatricesProbas_1() {
        System.out.println("Utils.convertirMatricesCumulsEnMatricesProbas_1");
        int[][] mInt = {{1, 6, 2}, {6, 2, 1}, {0, 3, 7}, {5, 4, 3}};
        double[][] m = Utils.convertirMatricesCumulsEnMatricesProbas(mInt, null, 0).getFirst();
        assertEquals(m.length, 4);
        assertEquals(m[0].length, 3);

        assertEquals(m[0][0], 1. / 9, 0.0000001);
        assertEquals(m[0][1], 6. / 9, 0.0000001);
        assertEquals(m[0][2], 2. / 9, 0.0000001);

        assertEquals(m[1][0], 6. / 9, 0.0000001);
        assertEquals(m[1][1], 2. / 9, 0.0000001);
        assertEquals(m[1][2], 1. / 9, 0.0000001);

        assertEquals(m[2][0], 0, 0.0000001);
        assertEquals(m[2][1], 3. / 10, 0.0000001);
        assertEquals(m[2][2], 7. / 10, 0.0000001);

        assertEquals(m[3][0], 5. / 12, 0.0000001);
        assertEquals(m[3][1], 4. / 12, 0.0000001);
        assertEquals(m[3][2], 3. / 12, 0.0000001);
    }

    /**
     * Test of convertirMatricesCumulsEnMatricesProbas method, of class Utils.
     */
    @Test
    public void testTonvertirMatricesCumulsEnMatricesProbas_2() {
        System.out.println("Utils.convertirMatricesCumulsEnMatricesProbas_2");
        int[][] mInt = {{1, 6, 2}, {6, 2, 1}, {0, 3, 7}, {5, 4, 3}};
        double[][] m = Utils.convertirMatricesCumulsEnMatricesProbas(mInt, null, 0.02).getFirst();
        assertEquals(m.length, 4);
        assertEquals(m[0].length, 3);

        assertEquals(m[0][0], 1. / 9 * 0.98, 0.0000001);
        assertEquals(m[0][1], 6. / 9 * 0.98, 0.0000001);
        assertEquals(m[0][2], 2. / 9 * 0.98, 0.0000001);

        assertEquals(m[1][0], 6. / 9 * 0.98, 0.0000001);
        assertEquals(m[1][1], 2. / 9 * 0.98, 0.0000001);
        assertEquals(m[1][2], 1. / 9 * 0.98, 0.0000001);

        assertEquals(m[2][0], 0, 0.0000001);
        assertEquals(m[2][1], 3. / 10 * 0.98, 0.0000001);
        assertEquals(m[2][2], 7. / 10 * 0.98, 0.0000001);

        assertEquals(m[3][0], 5. / 12 * 0.98, 0.0000001);
        assertEquals(m[3][1], 4. / 12 * 0.98, 0.0000001);
        assertEquals(m[3][2], 3. / 12 * 0.98, 0.0000001);
    }

    /**
     * Test of convertirMatricesCumulsEnMatricesProbas method, of class Utils.
     */
    @Test
    public void testTonvertirMatricesCumulsEnMatricesProbas_3() {
        System.out.println("Utils.convertirMatricesCumulsEnMatricesProbas_3");
        int[][] mInt1 = {{1, 6, 2}, {6, 2, 1}, {0, 3, 7}};
        int[][] mInt2 = {{12, 2}, {5, 1}, {0, 1}};
        LinkedList<double[][]> l = Utils.convertirMatricesCumulsEnMatricesProbas(mInt1, mInt2, 0);
        double[][] m1 = l.getFirst();
        double[][] m2 = l.getLast();

        assertEquals(m1.length, 3);
        assertEquals(m1[0].length, 3);
        assertEquals(m2.length, 3);
        assertEquals(m2[0].length, 2);

        assertEquals(m1[0][0], 1. / 23, 0.0000001);
        assertEquals(m1[0][1], 6. / 23, 0.0000001);
        assertEquals(m1[0][2], 2. / 23, 0.0000001);

        assertEquals(m1[1][0], 6. / 15, 0.0000001);
        assertEquals(m1[1][1], 2. / 15, 0.0000001);
        assertEquals(m1[1][2], 1. / 15, 0.0000001);

        assertEquals(m1[2][0], 0, 0.0000001);
        assertEquals(m1[2][1], 3. / 11, 0.0000001);
        assertEquals(m1[2][2], 7. / 11, 0.0000001);

        assertEquals(m2[0][0], 12. / 23, 0.0000001);
        assertEquals(m2[0][1], 2. / 23, 0.0000001);

        assertEquals(m2[1][0], 5. / 15, 0.0000001);
        assertEquals(m2[1][1], 1. / 15, 0.0000001);

        assertEquals(m2[2][0], 0, 0.0000001);
        assertEquals(m2[2][1], 1. / 11, 0.0000001);
    }

    /**
     * Test of convertirMatricesCumulsEnMatricesProbas method, of class Utils.
     */
    @Test
    public void testTonvertirMatricesCumulsEnMatricesProbas_4() {
        System.out.println("Utils.convertirMatricesCumulsEnMatricesProbas_4");
        int[][] mInt1 = {{1, 6, 2}, {6, 2, 1}, {0, 3, 7}};
        int[][] mInt2 = {{12, 2}, {5, 1}, {0, 1}};
        LinkedList<double[][]> l = Utils.convertirMatricesCumulsEnMatricesProbas(mInt1, mInt2, 0.4);
        double[][] m1 = l.getFirst();
        double[][] m2 = l.getLast();

        assertEquals(m1.length, 3);
        assertEquals(m1[0].length, 3);
        assertEquals(m2.length, 3);
        assertEquals(m2[0].length, 2);

        assertEquals(m1[0][0], 1. / 23 * .6, 0.0000001);
        assertEquals(m1[0][1], 6. / 23 * .6, 0.0000001);
        assertEquals(m1[0][2], 2. / 23 * .6, 0.0000001);

        assertEquals(m1[1][0], 6. / 15 * .6, 0.0000001);
        assertEquals(m1[1][1], 2. / 15 * .6, 0.0000001);
        assertEquals(m1[1][2], 1. / 15 * .6, 0.0000001);

        assertEquals(m1[2][0], 0, 0.0000001);
        assertEquals(m1[2][1], 3. / 11 * .6, 0.0000001);
        assertEquals(m1[2][2], 7. / 11 * .6, 0.000001);

        assertEquals(m2[0][0], 12. / 23 * .6, 0.0000001);
        assertEquals(m2[0][1], 2. / 23 * .6, 0.0000001);

        assertEquals(m2[1][0], 5. / 15 * .6, 0.0000001);
        assertEquals(m2[1][1], 1. / 15 * .6, 0.0000001);

        assertEquals(m2[2][0], 0, 0.0000001);
        assertEquals(m2[2][1], 1. / 11 * .6, 0.0000001);
    }

    /**
     * Test of convertirTableauArrayList_En_TableauTableau method, of class Utils.
     */
    @Test
    public void testConvertirTableauArrayList_En_TableauTableau() {
        System.out.println("Utils.convertirTableauArrayList_En_TableauTableau");
        ArrayList<Integer> al1 = new ArrayList<Integer>();
        ArrayList<Integer> al2 = new ArrayList<Integer>();
        ArrayList<Integer> al3 = new ArrayList<Integer>();

        al1.add(1);
        al1.add(5);
        al1.add(2);
        al1.add(-23);
        al2.add(10);
        al2.add(50);
        al2.add(20);
        al2.add(-230);
        al3.add(21);
        al3.add(25);
        al3.add(22);
        al3.add(-223);

        ArrayList[] al = {al1, al2, al3};
        int[][] t = Utils.convertirTableauArrayList_En_TableauTableau(al);

        assertEquals(t.length, 3);
        assertEquals(t[0].length, 4);
        assertEquals(t[0][0], 1);
        assertEquals(t[0][1], 5);
        assertEquals(t[0][2], 2);
        assertEquals(t[0][3], -23);
        assertEquals(t[1][0], 10);
        assertEquals(t[1][1], 50);
        assertEquals(t[1][2], 20);
        assertEquals(t[1][3], -230);
        assertEquals(t[2][0], 21);
        assertEquals(t[2][1], 25);
        assertEquals(t[2][2], 22);
        assertEquals(t[2][3], -223);
    }

    /**
     * Test of convertirMatriceDouble_String method, of class Utils.
     */
    @Test
    public void testConvertirMatriceDouble_String() {
        System.out.println("Utils.convertirMatriceDouble_String");
        double[][] t = {{2.3, 4, 5}, {6.5, 0, 1}};
        String s = Utils.convertirMatriceDouble_String(t);
        assertEquals(s, "6.5\t0.0\t1.0\n2.3\t4.0\t5.0\n");
    }

    /**
     * Test of convertirMatriceInt_String method, of class Utils.
     */
    @Test
    public void testConvertirMatriceInt_String() {
        System.out.println("Utils.convertirMatriceInt_String");
        int[][] t = {{2, 4, 5}, {6, 0, 1}};
        String s = Utils.convertirMatriceInt_String(t);
        assertEquals(s, "6\t0\t1\n2\t4\t5\n");
    }

    /**
     * Test of convertirTableauString_String method, of class Utils.
     */
    @Test
    public void tesConvertirTableauString_String() {
        System.out.println("Utils.convertirTableauString_String");
        String[] t = {"ij", "poje", "kkkkk"};
        String s = Utils.convertirTableauString_String(t);
        assertEquals(s, "ijpojekkkkk");
    }
}