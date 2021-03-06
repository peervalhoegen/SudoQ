package de.sudoq.model.solverGenerator.FastSolver;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import de.sudoq.model.files.FileManager;
import de.sudoq.model.files.FileManagerTests;
import de.sudoq.model.solverGenerator.GenerationAlgo;
import de.sudoq.model.sudoku.Field;
import de.sudoq.model.sudoku.Position;
import de.sudoq.model.sudoku.PositionMap;
import de.sudoq.model.sudoku.Sudoku;
import de.sudoq.model.sudoku.SudokuBuilder;
import de.sudoq.model.sudoku.complexity.Complexity;
import de.sudoq.model.sudoku.sudokuTypes.SudokuType;
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes;
import de.sudoq.model.xml.XmlHelper;

import static org.junit.Assert.assertEquals;


public class SamuraiTest {

    @BeforeClass
    public static void init() {
        FileManagerTests.init();
    }

    @Test
	public void testSolveSamurai() {
        for (SudokuTypes st : SudokuTypes.values())
            for (Complexity c : Complexity.playableValues())
                for (int i = 1; i <= 10; i++) {
                    Sudoku s = getSudoku(FileManager.getSudokuDir(), SudokuTypes.samurai, Complexity.easy, 1);
                    testOneSudoku(s);
                }
    }


    private void testOneSudoku(Sudoku s){
        FastSolver fs = FastSolverFactory.getSolver(s);

        if (fs.hasSolution()) {
            System.out.println("Fast finds solution");
            /*//print
            PositionMap<Integer> solution = fs.getSolutions();
            SudokuBuilder sub = new SudokuBuilder(s.getSudokuType());
            Sudoku sudoku = sub.createSudoku();
            for (Position p: GenerationAlgo.getPositions(sudoku)) {
                Field f = sudoku.getField(p);
                f.setCurrentValue(solution.get(p));
            }
            System.out.println(sudoku );*/
        }

        FastSolver bbs = new BranchAndBoundSolver(s);

        assertEquals("dlx solver comes to same result as our solver", fs.hasSolution(), bbs.hasSolution());
    }



    public static Sudoku getSudoku(java.io.File dir, SudokuTypes st, Complexity c, int i){
        java.io.File f = new java.io.File(dir, st.toString()
                + java.io.File.separator
                + c.toString()
                + java.io.File.separator
                + "sudoku_" + i + ".xml");

        Sudoku s = new Sudoku(SudokuType.getSudokuType(st));
        try {
            s.fillFromXml(new XmlHelper().loadXml(f));
            s.setComplexity(Complexity.arbitrary);//justincase
            return s;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
