package tictactoe;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Main_TTT {

    final static boolean DebugON = true; //set "true" to turn the debug mode ON
    // settings
    final static boolean sngPlayer = true;
    // reg. constants
    final static int size = 3;
    final static int nO = 0;
    final static int nX = 1;
    final static int nGen = 2;
    final static int status = size; // defined for winLines
    final static char mX = 'X';
    final static char mO = 'O';
    final static char mFree = '_';
    // game/move statuses
    final static String stNotFinished = "Game not finished";
    final static String stDraw = "Draw";
    final static String stXwins = "X wins";
    final static String stOwins = "O wins";
    final static String stImpossible = "Impossible";

    // Global vars
    static Scanner sysIn = new Scanner(System.in);
    static Random rand = new Random();

    static int nFoe = nO;
    static int nCmp = nX;
    static int player;      // current player
    static int[] moveCrd = {0, 0};     // i (row) & j (column) of last move
    static String gameStat = stNotFinished; // current status of Game - ret.code of checkStatus();
    static String moveStat = stNotFinished; // status of last move - ret.code of makeMove(); May keep only predefined values
    static char[][] board = new char[size][size]; // gaming board. cell cont.: mX | mO | mNONE
    static int[][][] winLines = {
            // probable win-lines [number] [cellN] [coord i & j]
            // when middle index (cellN) is equal to "status"
            // then instead of [coord i & j] that array stores the state of current winLine for nX & nO resp.:
            //                        -1 - no perspective (has foe marks)
            //                         0 - clean
            //                         1, 2, 3 - of own marks (3 marks is win)
            {{0, 0}, {0, 1}, {0, 2}, {0, 0}}, // horizontals
            {{1, 0}, {1, 1}, {1, 2}, {0, 0}},
            {{2, 0}, {2, 1}, {2, 2}, {0, 0}},
            {{0, 0}, {1, 0}, {2, 0}, {0, 0}}, // verticals
            {{0, 1}, {1, 1}, {2, 1}, {0, 0}},
            {{0, 2}, {1, 2}, {2, 2}, {0, 0}},
            {{0, 0}, {1, 1}, {2, 2}, {0, 0}}, // diagonals
            {{2, 0}, {1, 1}, {0, 2}, {0, 0}}
    };

    static int[][][] opps = new int[3][size][size]; // OPPortunities matrix
    // 1st index represents layer:
    //      nO - actual opportunities for player nO
    //      nX - ... for player nX
    //      nGen - initial state)
    // 2nd & 3rd indices - cell coordinates on the board (i & j)


//===========================================================================
//  === ENTRY POINT ===
//
    public static void main(String[] args) throws IOException {

        initBoard();
        if (DebugON) { printOpps(nGen);}
        printBoardMarked();

        if (sngPlayer) {
            System.out.println("You play \"O\", I play \"X\"");
            System.out.println("tossing a coin ... ");
                // who's first move?
            System.out.print("First move goes to ");
                // "toss a coin" to choose a player to move first
            if ( rand.nextBoolean() ) {
                System.out.println("ME");
                player = nCmp;  // nX
            } else {
                System.out.println("YOU");
                player = nFoe;  // nO
            }
        } else {  // MULTI-player
            player = nX;
        }
            // game cycle
        do {
        if (sngPlayer && player == nCmp) {
            moveCrd = getCmpMove();
            System.out.println("MY move is: "+(moveCrd[0]+1)+" "+(moveCrd[1]+1));
            System.out.println("Press <Enter> to continue ...");
            System.in.read();
        } else {
            moveCrd = getFoeMove();
        }
        makeMove(player, moveCrd);
        printBoardMarked();
        if (DebugON) { printOpps(nGen); }
        checkStatus();
        player = (player == nX) ? nO : nX;  // flip the player
    }
        while (gameStat.equals(stNotFinished));
            // wrapping up the game
        System.out.println(gameStat);
    }

//===========================================================================
//    P R O C E D U R E S
//===========================================================================

    static void initBoard() {
        fillBoardChar(mFree);
        buildOpps();
        // zeroize winLine statuses
        for (int line = 0; line < winLines.length; line++) {
            winLines[line][status][nX] = 0;
            winLines[line][status][nO] = 0;
        }
        // reset statuses
        gameStat = stNotFinished;
        moveStat = stNotFinished;
    }

    // fill board with the same character (like "_")
    static void fillBoardChar(char fillChar) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = fillChar;
            }
        }
    }

    // initial build of opps matrix based on winLines
    // then copy nGen to X and O layers
    static void buildOpps() {
        for (int line = 0; line < winLines.length; line++) {
            for (int cellN = 0; cellN < size; cellN++) {
                int i = winLines[line][cellN][0];
                int j = winLines[line][cellN][1];
                opps[nGen][i][j] += 1 << line;
            }
        }
        // copy nGen layer to nX & nO layers of opps
        for (int i = 0; i < size; i++) {
            System.arraycopy(opps[nGen][i], 0, opps[nO][i], 0, size);
            System.arraycopy(opps[nGen][i], 0, opps[nX][i], 0, size);
        }
    }

    static void printBoardMarked() {
        if ( getFreeCells().length == size*size ) {
            moveCrd[0] = moveCrd[1] = size;
        }
        System.out.println("    1 2 3  ");
        System.out.println(" ".repeat(2) + "-".repeat(9));
        for (int i = 0; i < size; i++) {
            System.out.print( (i+1)+" | ");
            for (int j = 0; j < size; j++) {
                System.out.print(board[i][j] + " ");
            }
            System.out.println(i == moveCrd[0] ? "<" : "|");
        }
        int posJ = 2 + moveCrd[1] * 2;
        System.out.println("  " + "-".repeat(posJ) + "^" + "-".repeat(9 - (posJ + 1)));
    }

    // Get player's (foe) next move coordinates
    // perform checks:
    //      - numeric input;
    //      - within the range 1..3;
    //      - if the square is free
    //  ret: int[2] array of {i, j} coordinates
    //
    static int[] getFoeMove() {
        int[] crd = new int[2];

        do {
            System.out.print("Enter the coordinates for \""+(player==nX?"X":"O")+"\" : ");
            String inpI = sysIn.next();
            String inpJ = sysIn.next();
            if (!isNumeric(inpI) || !isNumeric(inpJ)) {
                System.out.println("You should enter numbers!");
                continue;
            }

            crd[0] = Integer.parseInt(inpI);
            crd[1] = Integer.parseInt(inpJ);
            if ((crd[0] < 1 || crd[0] > size)
                    || (crd[1] < 1 || crd[1] > size)) {
                System.out.println("Coordinates should be from 1 to 3!");
                continue;
            }

            crd[0]--;
            crd[1]--;
            if (board[crd[0]][crd[1]] != mFree) {
                System.out.println("This cell is occupied! Choose another one!");
                continue;
            }
            break;
        }
        while (true);

        return crd;
    }

    // Make a move - UPDATE Opps & winLines with new move
    // params:
    //      i, j - last move cell location on the board ( {i, j} coordinates)
    //
    static void makeMove(int plr, int[] crd) {
        char moveMark = ( plr == nX ? mX : mO);
        if (board[crd[0]][crd[1]] != mFree) {
            System.out.println("makeMove: cell [" + crd[0] + "][" + crd[1] + "] isn't free for " + moveMark + " (quitting)");
            System.exit(-7);
        }
        board[crd[0]][crd[1]] = moveMark;
        moveCrd[0] = crd[0];
        moveCrd[1] = crd[1];
        int oppsMask = (opps[nO][crd[0]][crd[1]] | opps[nX][crd[0]][crd[1]]);
        if (oppsMask != 0) {
            int[] oppsList = getBitsB(oppsMask);
            for (int b : oppsList) {
                updWinLine(b, plr);
            }
        }
    }

    // update 1 line of winLines array given the coordinates of a move
    // params:
    //      line - winLine to update
    //      i, j - last move cell location on the board ( {i, j} coordinates)
    //
    static void updWinLine(int line, int plr) {
        winLines[line][status][plr] += winLines[line][status][plr] >= 0 ? 1 : 0;
        if (winLines[line][status][plr] == 3) {
            moveStat = (plr == nX ? stXwins : stOwins);
            gameStat = (gameStat.equals( (plr == nX?stOwins:stXwins) )) ? stImpossible : (plr == nX?stXwins:stOwins);
        }
        switch (plr) {
            case nX:
                winLines[line][status][nO] = -1;
                removeOppsByLine(line, nO);
                break;
            case nO:
                winLines[line][status][nX] = -1;
                removeOppsByLine(line, nX);
                break;
        }
    }

    // remove opportunities for line in opps matrix
    // for specified player
    static void removeOppsByLine(int line, int player) {
        int[] cellLoc;

        for (int i = 0; i < size; i++) {
            cellLoc = winLines[line][i];
            opps[player][cellLoc[0]][cellLoc[1]] = removeBitB(line, opps[player][cellLoc[0]][cellLoc[1]]);
        }
    }

    static boolean itsPossible() {
        // calculate mX and mO
        //
        int cntO = 0;
        int cntX = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                switch (board[i][j]) {
                    case mO:
                        cntO++;
                        break;
                    case mX:
                        cntX++;
                        break;
                    case mFree:
                        break;
                    default:
                        System.out.println("(FATAL!) Illegal symbol on the board " + board[i][j] + "- (quitting)");
                        System.exit(-7);
                }
            }
        }
        return Math.abs(cntO - cntX) <= 1;
    }

    // Check game status
    // no params
    //      sets global var gameStatus
    static void checkStatus() {
        if (!itsPossible()) {
            gameStat = stImpossible;
            return;
        }

        switch (gameStat) {
            case stNotFinished:
                if (getFreeCells().length == 0) {
                    gameStat = stDraw;
                }
                break;
            case stImpossible:
                return;
            case stOwins:
                return;
            case stXwins:
                return;
            case stDraw:
                return;
            default:
                System.out.println("(FATAL!) Illegal game status (" + gameStat + ") (quitting)");
                System.exit(-7);
        }
    }

    static int[][] getFreeCells() {
        int[][] tmpArr = new int[9][2];
        int len = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == mFree) {
                    tmpArr[len][0] = i;
                    tmpArr[len][1] = j;
                    len++;
                }
            }
        }
        int[][] arr = new int[len][2];
        for (int i = 0; i < len; i++) {
            System.arraycopy(tmpArr[i], 0, arr[i], 0, 2);
        }
        return arr;
    }

    // Figure out the best move for Computer
    //  ret.:  move coordinates {i, j} as array of int[2]
    static int[] getCmpMove() {
        if (DebugON) { System.out.println("Looking for next move !");}
        int[][] freeOpps;  // list of free cells with number of opps
                            // 2nd index:   0 & 1 - row & column of the cell
                            //              2 - number of AVAIL. Opps
        int[] crd;

        crd = checkCloseVictory(nCmp);
        if (crd.length != 0) {
            if (DebugON) { System.out.println(" >> victorious move = "+(crd[0]+1)+" "+(crd[1]+1) );}
            return crd;
        }
        crd = checkDefense2(nCmp);
        if (crd.length != 0) {
            if (DebugON) { System.out.println(" >> Danger! Defensive move = "+(crd[0]+1)+" "+(crd[1]+1) );}
            return crd;
        }

        if (DebugON) { System.out.println(" > looking for best vacant cell ...");}
        freeOpps = getFreeOpps(nCmp);
        sort2dIntArr( freeOpps, 2, -1 );
        if (freeOpps.length == 0) {
            if (DebugON) { System.out.println(" >> haven't found one - will use any free cell.");}
            return getFreeCells()[0];
        }
        if (DebugON) {
            System.out.println(" >> found ! move = "+(freeOpps[0][0]+1)+" "+(freeOpps[0][1]+1) );
        }

        return new int[] {freeOpps[0][0], freeOpps[0][1]};
    }

    // finds 1st foe's avail. winLine with 2 marks - immediate response !
    //  ret.: coordinates of move ( int[2] )
    //
    static int[] checkDefense2(int plr) {
        if (DebugON) { System.out.println(" > Checking the need of defensive move.");}
        int foe = (plr == nX ? nO : nX);

        for (int line = 0; line < winLines.length; line++) {
            if ( winLines[line][status][foe] == 2) {
                return findFreeCellInWinLine(line);
            }
        }
        return new int[] {};
    }

    // Checks if player may win next move
    //  ret.: coordinates of a move ( int[2] )
    //
    static int[] checkCloseVictory(int plr) {
        if (DebugON) { System.out.println(" > Checking for close victory ...");}

        for (int line = 0; line < winLines.length; line++) {
            if ( winLines[line][status][plr] == 2 ) {
                return findFreeCellInWinLine(line);
            }
        }
        return new int[] {};
    }

    static int[] findFreeCellInWinLine(int line) {

        for (int wlcl = 0; wlcl < size; wlcl++) {
            int row = winLines[line][wlcl][0];
            int col = winLines[line][wlcl][1];
            if (board[row][col] == mFree) {
                return new int[] {row, col};
            }
        }
        return new int[] {};
    }

    // Get the list of vacant (free) cells which belong to available (active) winLines of player "plr"
    //  ret.: array fOpps[num][0..2]
    //      1st ind - rec.number
    //      2nd ind -   0 & 1 - cell coordinates
    //                  2 - number of opportunities (avail. win-lines) this cell belongs to
    static int[][] getFreeOpps(int plr) {
        int[][] tmpArr = new int[9][3];
        int len = 0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == mFree && opps[plr][i][j] > 0) {
                    tmpArr[len][0] = i;
                    tmpArr[len][1] = j;
                    tmpArr[len++][2] = cntBitsB(opps[plr][i][j]);
                }
            }
        }
        if (len == 0) {
            return new int[][] { {} };
        }
        int[][] arr = new int[len][3];
        for (int i = 0; i < len; i++) {
            System.arraycopy(tmpArr[i], 0, arr[i], 0, 3);
        }
        return arr;
    }

/* ----------------------------------------------------------------------------
    GENERAL PURPOSE functions
   ----------------------------------------------------------------------------
 */
    // Get all "1" bits of a byte
    //      processes ONLY 1st byte of input param (mask)
    // returns an array of bit numbers
    static int[] getBitsB(int mask) {
        int i;
        int[] tmpArr = new int[32];
        int len = 0;
        for (i = 0; i < 8; i++) {
            if ((mask & 1) == 1) {
                tmpArr[len++] = i;
            }
            mask = (mask >>> 1);
        }
        int[] arr = new int[len];
        System.arraycopy(tmpArr, 0, arr, 0, len);
        return arr;
    }

    // Counts all "1" bits of a byte
    //      processes ONLY 1st byte of input param (mask)
    // returns: number of bits
    static int cntBitsB(int mask) {
        return getBitsB(mask).length;
    }

    // replaces bit number bitN with "0" in the mask
    static int removeBitB(int bitN, int mask) {
        if (bitN < 0 || bitN > 8) {
            System.out.println("removeBitB: out of range bit number !");
            System.exit(-7);
        }
        return mask & ~(1 << bitN);
    }

    // Sort 2-dimensional array of ints
    // params:      arr - array of ints
    //              ind2s - number of element (of 2nd index) to use as key for sorting
    //              ord - -1 - descending, 1 - ascending
    static void sort2dIntArr(int[][] arr, int ind2s, int ord) {
        int iSize = arr.length;
        if (iSize <= 1) {  // zero length or one-row array - nothing to sort !
            return;
        }
        int jSize = arr[0].length;
        if (jSize < 1) { // zero length (2nd index array - nothing to sort !
            return;
        }
        if (ind2s < 0 || ind2s > jSize) {  // sort key (ind2s) is out of 2nd index range;
            return;
        }

        int[] buff;
        boolean wasSwaped;
        for (int len1 = iSize; len1 > 1; len1--) {
            wasSwaped = false;
            for (int i = 1; i < len1; i++) {
                if ((arr[i - 1][ind2s] - arr[i][ind2s]) * ord > 0) {
                    buff = arr[i];
                    arr[i] = arr[i - 1];
                    arr[i - 1] = buff;
                    wasSwaped = true;
                }
            }
            if (!wasSwaped) {
                break;
            }
        }
    }

    // checks if string represents correct real or integer number
    static boolean isNumeric(String inp) {
        try {
            Double.parseDouble(inp);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // pads string with spaces, aligned to "Center"
    //      if string's length is greater than width - it'll be left-truncated
    //      if string is empty - returns string of spaces
    //      if number of padding spaces is odd - the left padding will be 1 space shorter
    // params:  str - string to pad
    //          width - resulting width
    static String padStrC(String str, int width) {
        int len = str.length();
        if (len == 0) {
            return " ".repeat(width);
        }
        if (len > width) {
            return str.substring(0, width);
        }

        return " ".repeat((width - len) / 2) + str + " ".repeat(width - (width - len) / 2 - len);
    }

/* ----------------------------------------------------------------------------
    for DEBUG ONLY
   ----------------------------------------------------------------------------
 */

    // Print Opps matrix
    // params:
    //          layer - if nO or nX - prints single matrix
    //                  if nGen - complex output : board, nO, nX and (!) WinLines counts for nO and nX
    static void printOpps(int layer) {
        if (!DebugON) {
            return;
        }
        int cols = 2;
        int rows = 15;
        int wdth = 42; //  must be true ( wdth % 3 == 0 )
        String[][] outp = new String[cols][rows];
        int strCnt0 = 0;
        int strCnt1 = 0;

        if (layer == nGen) {
            // (1/3) print board marked (column 0) (width = wdth) (5 lines)

            if ( getFreeCells().length == size*size ) {
                moveCrd[0] = moveCrd[1] = size;
            }

            outp[0][strCnt0] = padStrC("-".repeat(9), wdth);
            for (int i = 0; i < size; i++) {
                strCnt0++;
                outp[0][strCnt0] = "| ";
                for (int j = 0; j < size; j++) {
                    outp[0][strCnt0] += board[i][j] + " ";
                }
                outp[0][strCnt0] += (i == moveCrd[0] ? "<" : "|");
                outp[0][strCnt0] = padStrC(outp[0][strCnt0], wdth);
            }
            strCnt0++;
            int posJ = 2 + moveCrd[1] * 2;
            outp[0][strCnt0] = padStrC("-".repeat(posJ) + "^" + "-".repeat(9 - (posJ + 1)), wdth);

            // (1/3) print initial (nGen) Opps matrix (column 1) (width = wdth) (5 lines)
            outp[1][strCnt1] = padStrC("INITIAL Opps matrix", wdth);
            for (int i = 0; i < size; i++) {
                strCnt1++;
                outp[1][strCnt1] = "";
                for (int j = 0; j < size; j++) {
                    outp[1][strCnt1] += padStrC(Arrays.toString(getBitsB(opps[nGen][i][j])), wdth / 3);
                }
            }
            outp[1][++strCnt1] = " ".repeat(wdth);  // to skip 1 line ;-)  (column 1)

            outp[0][++strCnt0] = " ".repeat(wdth);   // to skip 1 line ;-)   (skip 1 line for both columns)
            outp[1][++strCnt1] = " ".repeat(wdth);   // to skip 1 line ;-)

            // (2/3) print (nO) Opps (column 0) (width = wdth) (4 lines)
            strCnt0++;
            outp[0][strCnt0] = padStrC("(nO) Opps", wdth);
            for (int i = 0; i < size; i++) {
                strCnt0++;
                outp[0][strCnt0] = "";
                for (int j = 0; j < size; j++) {
                    outp[0][strCnt0] += padStrC(Arrays.toString(getBitsB(opps[nO][i][j])), wdth / 3);
                }
            }

            // (2/3) print (nX) Opps (column 1) (width = wdth) (4 lines)
            strCnt1++;
            outp[1][strCnt1] = padStrC("(nX) Opps", wdth);
            for (int i = 0; i < size; i++) {
                strCnt1++;
                outp[1][strCnt1] = "";
                for (int j = 0; j < size; j++) {
                    outp[1][strCnt1] += padStrC(Arrays.toString(getBitsB(opps[nX][i][j])), wdth / 3);
                }
            }

            outp[0][++strCnt0] = " ".repeat(wdth);   // to skip 1 line ;-)   (skip 1 line for both columns)
            outp[1][++strCnt1] = " ".repeat(wdth);   // to skip 1 line ;-)

            // (3/3) print (nO) Opps (column 0) (width = wdth) (3 lines)
            for (int i = 0; i < size; i++) {
                strCnt0++;
                outp[0][strCnt0] = "";
                for (int j = 0; j < size; j++) {
                    outp[0][strCnt0] += "[" + cntBitsB(opps[nO][i][j]) + "] ";
                }
                outp[0][strCnt0] = padStrC(outp[0][strCnt0], wdth);
            }

            // (3/3) print (nO) Opps (column 1) (width = wdth) (3 lines)
            for (int i = 0; i < size; i++) {
                strCnt1++;
                outp[1][strCnt1] = "";
                for (int j = 0; j < size; j++) {
                    outp[1][strCnt1] += "[" + cntBitsB(opps[nX][i][j]) + "] ";
                }
                outp[1][strCnt1] = padStrC(outp[1][strCnt1], wdth);
            }
            outp[0][++strCnt0] = "-".repeat(wdth);   // to skip 1 line ;-)   (skip 1 line for both columns)
            outp[1][++strCnt1] = "-".repeat(wdth);   // to skip 1 line ;-)

            // Print it FINALLY OUT !!!
            for (int j = 0; j < rows; j++) {
                System.out.println(outp[0][j] + "|" + outp[1][j]);
            }

        } else {
            // Print single matrix a time (nX or nO)
            System.out.println("Opps matrix for " + (layer == nO ? "nO" : "nX"));
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    System.out.print(Arrays.toString(getBitsB(opps[layer][i][j])) + "  ");
                }
                System.out.println();
            }
        }
    }

}

