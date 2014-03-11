package fr.loria.synalp.jtrans.phonetiseur;

import fr.loria.synalp.jtrans.phonetiseur.SuitePhonemes;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class SuitePhonemesTest {

    private final String[] tPhonemes = {"a", "b", "O", "v", "o"};
    private SuitePhonemes sp = new SuitePhonemes(tPhonemes, "posTag");

    public SuitePhonemesTest() {
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
     * Test of getTPhonemes method, of class SuitePhonemes.
     */
    @Test
    public void testGetTPhonemes() {
        System.out.println("SuitePhonemes.getTPhonemes");
        assertEquals(sp.getTPhonemes().length, tPhonemes.length);
        for (int i = 0; i < tPhonemes.length; i++) {
            assertEquals(sp.getPhoneme(i), tPhonemes[i]);
        }
    }

    /**
     * Test of getPosTag method, of class SuitePhonemes.
     */
    @Test
    public void testGetPosTag() {
        System.out.println("SuitePhonemes.getPosTag");
        assertEquals(sp.getPosTag(), "posTag");
    }

    /**
     * Test of getNbPhonemes method, of class SuitePhonemes.
     */
    @Test
    public void testGetNbPhonemes() {
        System.out.println("SuitePhonemes.getNbPhonemes");
        assertEquals(sp.getNbPhonemes(), 5);
    }

    /**
     * Test of toStringDebug method, of class SuitePhonemes.
     */
    @Test
    public void testToStringDebug() {
        System.out.println("SuitePhonemes.toStringDebug");
        assertEquals(sp.toStringDebug(), "a b O v o (posTag)");
    }

    /**
     * Test of toString method, of class SuitePhonemes.
     */
    @Test
    public void testToString() {
        System.out.println("SuitePhonemes.toString");
        assertEquals(sp.toString(), "a b O v o ");
    }

    /**
     * Test of equals method, of class SuitePhonemes.
     */
    @Test
    public void testEquals() {
        System.out.println("SuitePhonemes.equals");
        SuitePhonemes spTest;
        String[] t = {"a", "b", "mmmm", "v", "o"};
        String[] tOk = {"a", "b", "O", "v", "o"};

        spTest = new SuitePhonemes(tOk, "");
        assertFalse(sp.equals(spTest));
        assertFalse(spTest.equals(sp));

        spTest = new SuitePhonemes(t, "posTag");
        assertFalse(sp.equals(spTest));
        assertFalse(spTest.equals(sp));

        spTest = new SuitePhonemes(t, "");
        assertFalse(sp.equals(spTest));
        assertFalse(spTest.equals(sp));

        spTest = new SuitePhonemes(tOk, "posTag");
        assertTrue(sp.equals(spTest));
        assertTrue(spTest.equals(sp));
    }
}