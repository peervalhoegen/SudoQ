package de.sudoq.model.solverGenerator;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import de.sudoq.model.files.FileManager;
import de.sudoq.model.solverGenerator.FastSolver.FastSolver;
import de.sudoq.model.solverGenerator.FastSolver.FastSolverFactory;
import de.sudoq.model.sudoku.Sudoku;
import de.sudoq.model.sudoku.complexity.Complexity;
import de.sudoq.model.sudoku.sudokuTypes.SudokuType;
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes;
import de.sudoq.model.xml.XmlHelper;

public class FromFileTest {

    //@Test
    //sudoku.xml file in question is empty (all solutions == -1), but just in case: this is how it would work
    public void testSamurai2fromFile(){
        Sudoku s = getSudoku("/home/t/Code/SudoQ/sudoq-app/sudoqmodel/src/test/java/de/sudoq/model/solverGenerator/FastSolver/infiniteLoopSudoku.xml", SudokuTypes.samurai);
        FastSolver fs = FastSolverFactory.getSolver(s);
        fs.isAmbiguous();

    }




    public static Sudoku getSudoku(String path, SudokuTypes st){
        FileManager.initialize(
                new File("/home/t/Code/SudoQ/DebugOnPC/profilefiles"),
                new File("/home/t/Code/SudoQ/sudoq-app/sudoqapp/src/main/assets/sudokus/"));
        File f = new File(path);

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
