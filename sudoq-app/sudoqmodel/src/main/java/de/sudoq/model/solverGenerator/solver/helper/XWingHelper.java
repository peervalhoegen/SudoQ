package de.sudoq.model.solverGenerator.solver.helper;

//TODO test this!!!

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.sudoq.model.solverGenerator.solution.DerivationBlock;
import de.sudoq.model.solverGenerator.solution.DerivationField;
import de.sudoq.model.solverGenerator.solution.SolveDerivation;
import de.sudoq.model.solverGenerator.solver.SolverSudoku;
import de.sudoq.model.solvingAssistant.HintTypes;
import de.sudoq.model.sudoku.Constraint;
import de.sudoq.model.sudoku.Field;
import de.sudoq.model.sudoku.Position;
import de.sudoq.model.sudoku.Utils;

/**Idea: We have 2 intersecting groups:
 *   I = I_only + intersection
 *   J = J_only + intersection
 *
 *   a note that within I only appears in intersection, locks the note for J:
 *   it has to be in intersection and cannot be in J_only
 *
 * Created by timo on 07.06.16.
 */
public class XWingHelper extends SolveHelper {

    private static List a;
    private static List b;

    /**
	 * Erzeugt einen neuen HiddenHelper für das spezifizierte Suduoku.
     * Idea:
     *   We have a '#' of 2 rows+2columns.
     *   The 4 intersection fields all feature a common note n.
     *   for the 2 rows (the 2 columns) the note appears only in the intersection i.e. is locked there
     *   therefore the columns (the rows) have them in the intersection too and n can be deleted everywhere else.
	 *
	 * @param sudoku
	 *            Das Sudoku auf dem dieser Helper operieren soll
	 * @param complexity
	 *            Die Schwierigkeit der Anwendung dieser Vorgehensweise
	 * @throws IllegalArgumentException
	 *             Wird geworfen, falls das Sudoku null oder das level oder die complexity kleiner oder gleich 0 ist
	 *
     */
	public XWingHelper(SolverSudoku sudoku, int complexity) {
		super(sudoku, complexity);
	}

    private void separateIntoRowColumn(List<Constraint> pool, List<Constraint> rows, List<Constraint> cols){
        for(Constraint c: pool) {
            switch (Utils.getGroupShape(c.getPositions())) {
                case Row:
                    rows.add(c);
                    break;
                case Column:
                    cols.add(c);
                    break;
            }
        }
    }


    @Override
    public boolean update(boolean buildDerivation) {

        List<Constraint> constraints = sudoku.getSudokuType().getConstraints();

        /* collect rows / cols */
        List<Constraint> rows = new ArrayList<>();
        List<Constraint> cols = new ArrayList<>();
        separateIntoRowColumn(constraints, rows, cols);

        /*
        mapping from pos to row/col-constraints
        */

        /* compare all constraints, look for a '#': 2 rows, 2 col intersecting
        * move clockwise starting north-west i.e. topLeft*/
        for(int c1=0; c1< cols.size()-1; c1++)
            for(int r1=0; r1< rows.size()-1; r1++){
                Constraint col1 = cols.get(c1);
                Constraint row1 = rows.get(r1);
                Position topLeft = intersectionPoint(row1, col1);
                if(topLeft==null || !sudoku.getField(topLeft).isEmpty())
                    continue;

                for(int c2=c1+1; c2< cols.size(); c2++){
                    Constraint col2 = cols.get(c2);

                    Position topRight = intersectionPoint(row1, col2);
                    if(topRight==null || !sudoku.getField(topRight).isEmpty())
                        continue;

                    for(int r2=r1+1; r2< rows.size(); r2++) {
                        Constraint row2 = rows.get(r2);
                        Position bottomRight = intersectionPoint(col2, row2);
                        Position bottomLeft  = intersectionPoint(row2, col1);

                        if (bottomRight != null && sudoku.getField(bottomRight).isEmpty()
                         && bottomLeft  != null && sudoku.getField(bottomLeft).isEmpty()) {
                            /* we found a # of 2rows, 2 cols*/

                            if(r1==1 && r2==4 && c1==4 && c2==7)
                                System.out.println("we're there");


                            BitSet candidateNotes = intersectNotes(Arrays.asList(topLeft, topRight, bottomRight, bottomLeft));
                            for (int note = candidateNotes.nextSetBit(0); note >= 0; note = candidateNotes.nextSetBit(note + 1)) {

                                if      (xWing(row1, row2, col1, col2, note, new Position[]{topLeft, topRight, bottomLeft, bottomRight}, buildDerivation))
                                    return true;
                                else if (xWing(col1, col2, row1, row2, note, new Position[]{topLeft, topRight, bottomLeft, bottomRight}, buildDerivation))
                                    return true;

                            }
                        }
                    }
                }
            }
        return false;
    }

    private boolean xWing(Constraint row1,
                          Constraint row2,
                          Constraint col1,
                          Constraint col2,
                          int note,
                          Position[] corners,
                          boolean buildDerivation){
        //Xwing: row1, row2 haben in den schnittpunkten eine note die bez. Zeile nur dort vorkommt.
        //       note is therefor locked, can be deletd elsewhere in col1,col2
        boolean rowLocked = countNote((short) note, row1) == 2 && countNote((short) note, row2) == 2;
        boolean removableStuffInColumns = countNote((short) note, col1) > 2 || countNote((short) note, col2) > 2;
        if ( rowLocked && removableStuffInColumns ) {

            List<Position> canBeDeleted = new ArrayList<>();
            for (Position p : col1.getPositions())
                if (sudoku.getCurrentCandidates(p).get(note))
                    canBeDeleted.add(p);
            for (Position p : col2)
                if (sudoku.getCurrentCandidates(p).get(note))
                    canBeDeleted.add(p);

            for (Position p: corners)
                canBeDeleted.remove(p);

            /* delete notes */
            for (Position p : canBeDeleted)
                sudoku.getCurrentCandidates(p).clear(note);


            if (buildDerivation) {

                lastDerivation = new SolveDerivation(HintTypes.XWing);
                lastDerivation.addDerivationBlock(new DerivationBlock(row1));
                lastDerivation.addDerivationBlock(new DerivationBlock(row2));
                lastDerivation.addDerivationBlock(new DerivationBlock(col1));
                lastDerivation.addDerivationBlock(new DerivationBlock(col2));
                for(Position p: canBeDeleted) {
                    BitSet relevant = new BitSet();
                    relevant.set(note);
                    BitSet irrelevant = (BitSet) relevant.clone();
                    for (int bit = irrelevant.nextSetBit(0); bit >= 0; bit = irrelevant.nextSetBit(bit + 1)) {
                        irrelevant.flip(bit);
                    }

                    lastDerivation.addDerivationField(new DerivationField(p, relevant, irrelevant));
                }

                StringBuilder sb = new StringBuilder();

                List<String> positionsS = new ArrayList<>();//TODO Mapping in scala
                for(Position pos : canBeDeleted)
                    positionsS.add(Utils.positionToRealWorld(pos).toString());

                lastDerivation.setDescription("We have an X-Wing with: " + Utils.classifyGroup(row1.getPositions()) + ", "
                                                                       + Utils.classifyGroup(row2.getPositions()) + ", "
                                                                       + Utils.classifyGroup(col1.getPositions()) + ", "
                                                                       + Utils.classifyGroup(col2.getPositions()) + ". "
                                                                       + "and note "+ note + ". "+note+"can thus be removed from the fields: "
                                                                       + String.join(", ", positionsS)  + ".\n");
            }
            return true;
        }
        else
            return false;

    }



    /**
     * Determines whether Lists a,b have a common(by equals) element
     * @param a
     * @param b
     * @param <T> any element in the list needs to have equals defined
     * @return true iff i.equals(j) == true for at least one i € a, j € b
     */
    private static <T> boolean intersect(List<T> a, List<T> b){

        for (T t1: a)
            for (T t2: b)
                if(t1.equals(t2))
                    return true;

        return false;
    }

    /** TODO some sort of Maybe would be better than returning null...
     * Determines the first T found to occur in a and b (by equals())
     * @param a
     * @param b
     * @param <T> any element in the list needs to have equals defined
     * @return an element i where i.equals(j) for  i € a, j € b, null iff none is found
     */
    private static <T> T intersectionPoint(Iterable<T> a, Iterable<T> b){

        for (T t1: a)
            for (T t2: b)
                if(t1.equals(t2))
                    return t1;

        return null;
    }

    private static <T> List<T> intersection(List<T> a, List<T> b){
        List <T> intersection = new ArrayList<>();
        for (T t1: a)
            for (T t2: b)
                if(t1.equals(t2))
                    intersection.add(t1);

        return intersection;
    }

    private static <T> List<T> cut(List<T> a, List<T> b){
        List <T> cut = new ArrayList<>(a);
        cut.removeAll(b);
        return cut;
    }

    private BitSet collectNotes(List<Position> l){
        if(l.isEmpty())
            return new BitSet();
        else{
            BitSet merged = new BitSet();
            for(Position p: l)
                merged.or(sudoku.getCurrentCandidates(p));
            return merged;
        }

    }

    private BitSet intersectNotes(List<Position> l){
        if(l.isEmpty())
            return new BitSet();
        else{
            BitSet merged = sudoku.getCurrentCandidates(l.get(0));
            BitSet b      = sudoku.getCurrentCandidates(l.get(1));
            BitSet c      = sudoku.getCurrentCandidates(l.get(2));
            BitSet d      = sudoku.getCurrentCandidates(l.get(3));
            for (int i = 1; i < l.size(); i++) {
                merged.and(sudoku.getCurrentCandidates(l.get(i))); //TODO in scala this could be a fold1 after mapping sudoku.getc..
            }

            return merged;
        }
    }

    private short countNote(short note, Iterable<Position> positions){
        short sum =0;
        for (Position p:positions)
            if(sudoku.getCurrentCandidates(p).get(note))
                sum++;

        return sum;
    }
}



