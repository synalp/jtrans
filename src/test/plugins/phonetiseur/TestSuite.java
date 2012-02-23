package test.plugins.phonetiseur;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    UtilsTest.class,
    AlignementGraphemesPhonemesTest.class,
    DicoGraphemesPhonemesTest.class,
    LexiqueGraphemesPhonemesPostagTest.class,
    SuitePhonemesTest.class,
    PhonetiseurTest.class,
    ClassifieursTest.class})
public class TestSuite {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }
}