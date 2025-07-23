import java.util.ArrayList;
import java.util.List;

public class Move {
    final private static long FILE_GH=217020518514230019L; // mask for fileG and H
    final private static long FILE_AB=-4557430888798830400L; // mask for fileA and B
    final private static long[] RankMasks =// from rank1 to rank8
            {
                    0xFFL, 0xFF00L, 0xFF0000L, 0xFF000000L, 0xFF00000000L, 0xFF0000000000L, 0xFF000000000000L, 0xFF00000000000000L
            };
    final private static long[] FileMasks =// from fileA to FileH
            {
                    0x101010101010101L, 0x202020202020202L, 0x404040404040404L, 0x808080808080808L,
                    0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L, 0x8080808080808080L
            };
    final private static long[] DiagonalMasks =// diagonal masks from top left to bottom right
            {
                    0x1L, 0x102L, 0x10204L, 0x1020408L, 0x102040810L, 0x10204081020L, 0x1020408102040L,
                    0x102040810204080L, 0x204081020408000L, 0x408102040800000L, 0x810204080000000L,
                    0x1020408000000000L, 0x2040800000000000L, 0x4080000000000000L, 0x8000000000000000L
            };
    final private static long[] AntiDiagonalMasks =// diagonal masks from top right to bottom left
            {
                    0x80L, 0x8040L, 0x804020L, 0x80402010L, 0x8040201008L, 0x804020100804L, 0x80402010080402L,
                    0x8040201008040201L, 0x4020100804020100L, 0x2010080402010000L, 0x1008040201000000L,
                    0x804020100000000L, 0x402010000000000L, 0x201000000000000L, 0x100000000000000L
            };

    /* The methods below are used to see the attack range of each type of the piece.  */

    // Method to get possible knight moves for all knights
    public static long getKnightMoves(long n, long sameOccupied){
        long nm = 0L; // possible knight moves
        while(n!=0L) {
            long m = Long.highestOneBit(n); // single out one of the knights
            long fnm = getSKnightMoves(m, sameOccupied);
            nm |= fnm;
            n = (~m) & n; // remove the knight position
        }
        return nm;
    }

    public static long getPawnMoves(long myPawns, long sameOccupied, long otherOccupied, boolean whiteToMove){
        long pawnMoves = 0L;
        while (myPawns != 0L){
            long pawn = Long.highestOneBit(myPawns);
            pawnMoves |= getSPawnMoves(pawn, sameOccupied, otherOccupied, whiteToMove);
            myPawns = (~pawn) & myPawns;
        }
        return pawnMoves;
    }
    
    // Method to get possible moves for a white pawn's capture
    public static long getWhitePawnCapture(long pawn, long otherOccupied){
        return (pawn << 7 | pawn << 9) & (otherOccupied); // shift pawn up diagonally as long as there is a black piece
    }

    //Method to get possible moves for a black pawn's capture
    public static long getBlackPawnCapture(long pawn, long sameOccupied, long otherOccupied){
        return (pawn >> 7 | pawn >> 9) & (otherOccupied); // shift pawn up diagonally as long as there is a black piece
    }

    // Method to get possible moves for all bishops
    public static Long getBishopMoves(long b,long sameOccupied, long otherOccupied) {
        long bm = 0L;
        while(b!=0L){
            long m = Long.highestOneBit(b);
            long fbm = getSBishopMoves(m, sameOccupied, otherOccupied);
            bm |= fbm;
            b = (~m) & b;
        }
        return bm;
    }

    // Method to get possible moves for all rooks
    public static Long getRookMoves(long r,long sameOccupied, long otherOccupied) {
        long rm = 0L;
        while(r!=0L){
            long m = Long.highestOneBit(r);
            long frm = getSRookMoves(r, sameOccupied, otherOccupied);
            rm |= frm;
            r = (~m) & r;
        }
        return rm;
    }

    // Method to get possible moves for all bishops
    public static Long getQueenMoves(long q,long sameOccupied, long otherOccupied) {
        long qm = 0L;
        long fqm = getRookMoves(q, sameOccupied, otherOccupied) | getBishopMoves(q,sameOccupied, otherOccupied);
        qm |= fqm;
        return qm;
    }

    /* The methods below are used to generate moves for evaluation and search process. */

    // Method to get possible moves for a single knight
    public static long getSKnightMoves(long n, long sameOccupied){ // single piece
        long fnm = (n << 6 | n << 10 | n << 15 | n << 17 | n >> 6 | n >> 10 | n >> 15 | n >> 17) & (~sameOccupied); // get possible knight moves
        // check if the piece is on the edge
        if (n%8 < 4) {
            fnm &= (~sameOccupied)&(~FILE_GH);
        }
        else {
            fnm &= (~sameOccupied)&(~FILE_AB);
        }
        return fnm;
    }

    // Method to get possible moves for a single pawn
    public static long getSPawnMoves(long pawn, long sameOccupied, long otherOccupied, boolean whiteToMove){
        long fPawnMoves;
        if (whiteToMove) {
            long singleStep = pawn << 8 & (~sameOccupied) & (~otherOccupied); // shift pawn up a rank
            long doubleStep = singleStep << 8 & (~sameOccupied) & (~otherOccupied); // shift pawn up two ranks
            long capture = (pawn << 7 | pawn << 9) & (otherOccupied); // shift pawn up diagonally as long as there is a black piece
            if (pawn <= 32768) { // if pawn is located on first rank
                fPawnMoves = singleStep | doubleStep | capture;
            } else {
                fPawnMoves = singleStep | capture;
            }
        } else {
            long singleStep = pawn >> 8 & (~sameOccupied) & (~otherOccupied); // shift pawn down a rank
            long doubleStep = singleStep >> 8 & (~sameOccupied) & (~otherOccupied); // shift pawn down two ranks
            long capture = (pawn >> 7 | pawn >> 9) & (otherOccupied); // shift pawn down diagonally as long as there is a white piece
            if ((pawn & 0xFF000000000000L) != 0) { // if pawn is located on 7th rank
                fPawnMoves = singleStep | doubleStep | capture;
            } else {
                fPawnMoves = singleStep | capture;
            }
        }
        int file = Long.numberOfTrailingZeros(pawn) % 8;
        if (file < 2) {
            fPawnMoves &= ~FILE_AB;  // Mask left wraparound
        } else if (file > 5) {
            fPawnMoves &= ~FILE_GH;  // Mask right wraparound
        }

        return fPawnMoves;
    }

    // Method to get possible moves for a single king
    public static long getSKingMoves(long k, long sameOccupied){
        long fkm = (k << 1 | k << 7 | k << 8 | k << 9 | k >> 1 | k >> 7 | k >> 8 | k >> 9 );
        int file = Long.numberOfTrailingZeros(k) % 8;
        if (file < 2) {
            fkm &= ~FILE_AB;  // Mask left wraparound
        } else if (file > 5) {
            fkm &= ~FILE_GH;  // Mask right wraparound
        }

        return fkm;
    }

    // Method to get possible moves for a single bishop
    public static Long getSBishopMoves(long b,long sameOccupied, long otherOccupied) {
        int s = Long.numberOfTrailingZeros(b);
        long o = sameOccupied | otherOccupied;
        long possibilitiesDiagonal = (((o&DiagonalMasks[(s / 8) + (s % 8)]) - (2 * b)) ^ Long.reverse(Long.reverse(o&DiagonalMasks[(s / 8) + (s % 8)]) - (2 * Long.reverse(b))))&DiagonalMasks[(s / 8) + (s % 8)];
        long possibilitiesAntiDiagonal = (((o&AntiDiagonalMasks[(s / 8) + 7 - (s % 8)]) - (2 * b)) ^ Long.reverse(Long.reverse(o&AntiDiagonalMasks[(s / 8) + 7 - (s % 8)]) - (2 * Long.reverse(b))))&AntiDiagonalMasks[(s / 8) + 7 - (s % 8)];
        long fbm = (possibilitiesDiagonal)&~sameOccupied | (possibilitiesAntiDiagonal)&~sameOccupied; // exclude the white pieces
        return fbm;
    }

    // Method to get possible moves for a single rook
    public static Long getSRookMoves(long r,long sameOccupied, long otherOccupied) {
        long rm = 0L;
        long m = Long.highestOneBit(r);
        int s = Long.numberOfTrailingZeros(m);
        long o = sameOccupied | otherOccupied;
        long possibilitiesHorizontal = ((o - 2 * m) ^ Long.reverse(Long.reverse(o) - 2 * Long.reverse(m)))&RankMasks[(s / 8)];
        long possibilitiesVertical = (((o&FileMasks[s % 8]) - (2 * m)) ^ Long.reverse(Long.reverse(o&FileMasks[s % 8]) - (2 * Long.reverse(m))))&FileMasks[(s % 8)];
        long frm = (possibilitiesHorizontal)&~sameOccupied | (possibilitiesVertical)&~sameOccupied; // exclude the white pieces
        rm |= frm;
        return rm;
    }

    // Method to get possible moves for a single queen
    public static Long getSQueenMoves(long q,long sameOccupied, long otherOccupied) {
        long qm = 0L;
        long fqm = getSRookMoves(q, sameOccupied, otherOccupied) | getSBishopMoves(q,sameOccupied, otherOccupied);
        qm |= fqm;

        return qm;
    }

    /**
     * Get all the possible moves from a given chessboard and store them in an array list.
     *
     * @param chessBoard the state of the given chess board.
     * @param whiteToMove The current player (true for white, false for black).
     * @return a collection of all legal moves from the given chessboard.
     */
    public static List<ChessBoard> getAllMoves(ChessBoard chessBoard, boolean whiteToMove) {
        // initialize the move list
        List<ChessBoard> allMoves = new ArrayList<>();

        // initialize the bitboards
        long ownPawns, ownKnights, ownBishops, ownRooks, ownQueens, ownKing;
        long opponentPawns, opponentKnights, opponentBishops, opponentRooks, opponentQueens, opponentKing;

        // assign
        if (whiteToMove) {
            ownPawns = chessBoard.getWP();
            ownKnights = chessBoard.getWN();
            ownBishops = chessBoard.getWB();
            ownRooks = chessBoard.getWR();
            ownQueens = chessBoard.getWQ();
            ownKing = chessBoard.getWK();

            opponentPawns = chessBoard.getBP();
            opponentKnights = chessBoard.getBN();
            opponentBishops = chessBoard.getBB();
            opponentRooks = chessBoard.getBR();
            opponentQueens = chessBoard.getBQ();
            opponentKing = chessBoard.getBK();
        } else {
            ownPawns = chessBoard.getBP();
            ownKnights = chessBoard.getBN();
            ownBishops = chessBoard.getBB();
            ownRooks = chessBoard.getBR();
            ownQueens = chessBoard.getBQ();
            ownKing = chessBoard.getBK();

            opponentPawns = chessBoard.getWP();
            opponentKnights = chessBoard.getWN();
            opponentBishops = chessBoard.getWB();
            opponentRooks = chessBoard.getWR();
            opponentQueens = chessBoard.getWQ();
            opponentKing = chessBoard.getWK();
        }

        Long[] opponentBoard = {opponentPawns, opponentKnights, opponentKing, opponentRooks, opponentBishops, opponentQueens};
        long ownPieces = ownPawns | ownKnights | ownBishops | ownRooks | ownQueens | ownKing;
        long opponentPieces = opponentPawns | opponentKnights | opponentBishops | opponentRooks | opponentQueens | opponentKing;

        // Save the initial board state
        long initialOwnPawns = ownPawns;
        long initialOwnKnights = ownKnights;
        long initialOwnBishops = ownBishops;
        long initialOwnRooks = ownRooks;
        long initialOwnQueens = ownQueens;
        long initialOwnKing = ownKing;

        // Save the initial board state
        chessBoard = new ChessBoard(ownPawns, ownKnights, ownBishops, ownRooks, ownQueens, ownKing, opponentPawns, opponentKnights, opponentBishops, opponentRooks, opponentQueens, opponentKing);


        // Get knight moves
        if (initialOwnKnights != 0L) {
            long knights = initialOwnKnights;
            while (knights != 0L) {
                long knight = Long.highestOneBit(knights);
                long knightMoves = getSKnightMoves(knight, ownPieces);
                while (knightMoves != 0L) {
                    long move = Long.highestOneBit(knightMoves);
                    // Check capture
                    for (int i = 0; i < 6; i++) {
                        long attacked = opponentBoard[i];
                        if ((move & attacked) != 0L) {
                            attacked = (~move) & attacked;
                            switch (i) {
                                case 0 -> opponentPawns = attacked;
                                case 1 -> opponentKnights = attacked;
                                case 2 -> opponentKing = attacked;
                                case 3 -> opponentRooks = attacked;
                                case 4 -> opponentBishops = attacked;
                                case 5 -> opponentQueens = attacked;
                            }
                        }
                    }
                    ownKnights = ownKnights & (~knight) | move;
                    allMoves.add(new ChessBoard(ownPawns, ownKnights, ownBishops, ownRooks, ownQueens, ownKing, opponentPawns, opponentKnights, opponentBishops, opponentRooks, opponentQueens, opponentKing));
                    ownKnights = initialOwnKnights;
                    opponentPawns = chessBoard.getBP();
                    opponentKnights = chessBoard.getBN();
                    opponentBishops = chessBoard.getBB();
                    opponentRooks = chessBoard.getBR();
                    opponentQueens = chessBoard.getBQ();
                    opponentKing = chessBoard.getBK();
                    knightMoves = (~move) & knightMoves;
                }
                knights = (~knight) & knights;
            }
        }
        // Get bishop moves
        if (initialOwnBishops != 0L) {
            long bishops = initialOwnBishops;
            while (bishops != 0L) {
                long bishop = Long.highestOneBit(bishops);
                long bishopMoves = getSBishopMoves(bishop, ownPieces, opponentPieces);
                while (bishopMoves != 0L) {
                    long move = Long.highestOneBit(bishopMoves);
                    // Check capture
                    for (int i = 0; i < 6; i++) {
                        long attacked = opponentBoard[i];
                        if ((move & attacked) != 0L) {
                            attacked = (~move) & attacked;
                            switch (i) {
                                case 0 -> opponentPawns = attacked;
                                case 1 -> opponentKnights = attacked;
                                case 2 -> opponentKing = attacked;
                                case 3 -> opponentRooks = attacked;
                                case 4 -> opponentBishops = attacked;
                                case 5 -> opponentQueens = attacked;
                            }
                        }
                    }
                    ownBishops = ownBishops & (~bishop) | move;
                    allMoves.add(new ChessBoard(ownPawns, ownKnights, ownBishops, ownRooks, ownQueens, ownKing, opponentPawns, opponentKnights, opponentBishops, opponentRooks, opponentQueens, opponentKing));
                    ownBishops = initialOwnBishops;
                    opponentPawns = chessBoard.getBP();
                    opponentKnights = chessBoard.getBN();
                    opponentBishops = chessBoard.getBB();
                    opponentRooks = chessBoard.getBR();
                    opponentQueens = chessBoard.getBQ();
                    opponentKing = chessBoard.getBK();
                    bishopMoves = (~move) & bishopMoves;
                }
                bishops = (~bishop) & bishops;
            }
        }

        // Get rook moves
        if (initialOwnRooks != 0L) {
            long rooks = initialOwnRooks;
            while (rooks != 0L) {
                long rook = Long.highestOneBit(rooks);
                long rookMoves = getSRookMoves(rook, ownPieces, opponentPieces);
                while (rookMoves != 0L) {
                    long move = Long.highestOneBit(rookMoves);
                    // Check capture
                    for (int i = 0; i < 6; i++) {
                        long attacked = opponentBoard[i];
                        if ((move & attacked) != 0L) {
                            attacked = (~move) & attacked;
                            switch (i) {
                                case 0 -> opponentPawns = attacked;
                                case 1 -> opponentKnights = attacked;
                                case 2 -> opponentKing = attacked;
                                case 3 -> opponentRooks = attacked;
                                case 4 -> opponentBishops = attacked;
                                case 5 -> opponentQueens = attacked;
                            }
                        }
                    }
                    ownRooks = ownRooks & (~rook) | move;
                    allMoves.add(new ChessBoard(ownPawns, ownKnights, ownBishops, ownRooks, ownQueens, ownKing, opponentPawns, opponentKnights, opponentBishops, opponentRooks, opponentQueens, opponentKing));
                    ownRooks = initialOwnRooks;
                    opponentPawns = chessBoard.getBP();
                    opponentKnights = chessBoard.getBN();
                    opponentBishops = chessBoard.getBB();
                    opponentRooks = chessBoard.getBR();
                    opponentQueens = chessBoard.getBQ();
                    opponentKing = chessBoard.getBK();
                    rookMoves = (~move) & rookMoves;
                }
                rooks = (~rook) & rooks;
            }
        }
        if (initialOwnQueens != 0L) {
            long queens = initialOwnQueens;
            while (queens != 0L) {
                long queen = Long.highestOneBit(queens);
                long queenMoves = getSQueenMoves(queen, ownPieces, opponentPieces);
                while (queenMoves != 0L) {
                    long move = Long.highestOneBit(queenMoves);
                    // Check capture
                    for (int i = 0; i < 6; i++) {
                        long attacked = opponentBoard[i];
                        if ((move & attacked) != 0L) {
                            attacked = (~move) & attacked;
                            switch (i) {
                                case 0 -> opponentPawns = attacked;
                                case 1 -> opponentKnights = attacked;
                                case 2 -> opponentKing = attacked;
                                case 3 -> opponentRooks = attacked;
                                case 4 -> opponentBishops = attacked;
                                case 5 -> opponentQueens = attacked;
                            }
                        }
                    }
                    ownQueens = ownQueens & (~queen) | move;
                    allMoves.add(new ChessBoard(ownPawns, ownKnights, ownBishops, ownRooks, ownQueens, ownKing, opponentPawns, opponentKnights, opponentBishops, opponentRooks, opponentQueens, opponentKing));
                    ownQueens = initialOwnQueens;
                    opponentPawns = chessBoard.getBP();
                    opponentKnights = chessBoard.getBN();
                    opponentBishops = chessBoard.getBB();
                    opponentRooks = chessBoard.getBR();
                    opponentQueens = chessBoard.getBQ();
                    opponentKing = chessBoard.getBK();
                    queenMoves = (~move) & queenMoves;
                }
                queens = (~queen) & queens;
            }
        }

        // Get king moves
        if (initialOwnKing != 0L) {
            long kings = initialOwnKing;
            while (kings != 0L) {
                long king = Long.highestOneBit(kings);
                long kingMoves = getSKingMoves(king, ownPieces);
                while (kingMoves != 0L) {
                    long move = Long.highestOneBit(kingMoves);
                    // Check capture
                    for (int i = 0; i < 6; i++) {
                        long attacked = opponentBoard[i];
                        if ((move & attacked) != 0L) {
                            attacked = (~move) & attacked;
                            switch (i) {
                                case 0 -> opponentPawns = attacked;
                                case 1 -> opponentKnights = attacked;
                                case 2 -> opponentKing = attacked;
                                case 3 -> opponentRooks = attacked;
                                case 4 -> opponentBishops = attacked;
                                case 5 -> opponentQueens = attacked;
                            }
                        }
                    }
                    ownKing = ownKing & (~king) | move;
                    allMoves.add(new ChessBoard(ownPawns, ownKnights, ownBishops, ownRooks, ownQueens, ownKing, opponentPawns, opponentKnights, opponentBishops, opponentRooks, opponentQueens, opponentKing));
                    ownKing = initialOwnKing;
                    opponentPawns = chessBoard.getBP();
                    opponentKnights = chessBoard.getBN();
                    opponentBishops = chessBoard.getBB();
                    opponentRooks = chessBoard.getBR();
                    opponentQueens = chessBoard.getBQ();
                    opponentKing = chessBoard.getBK();
                    kingMoves = (~move) & kingMoves;
                }
                kings = (~king) & kings;
            }
        }

        // Get pawn moves
        if (initialOwnPawns != 0L) {
            long pawns = initialOwnPawns;
            while (pawns != 0L) {
                long pawn = Long.highestOneBit(pawns);
                long pawnMoves = getSPawnMoves(pawn, ownPieces, opponentPieces, whiteToMove);
                while (pawnMoves != 0L) {
                    long move = Long.highestOneBit(pawnMoves);
                    // Check capture
                    for (int i = 0; i < 6; i++) {
                        long attacked = opponentBoard[i];
                        if ((move & attacked) != 0L) {
                            attacked = (~move) & attacked;
                            switch (i) {
                                case 0 -> opponentPawns = attacked;
                                case 1 -> opponentKnights = attacked;
                                case 2 -> opponentKing = attacked;
                                case 3 -> opponentRooks = attacked;
                                case 4 -> opponentBishops = attacked;
                                case 5 -> opponentQueens = attacked;
                            }
                        }
                    }
                    // Check promotion
                    if(((move & RankMasks[7]) | move & RankMasks[0]) != 0L){
                        ownQueens = ownQueens | move;
                        ownPawns = ownPawns & (~pawn);
                    }
                    else{ownPawns = ownPawns & (~pawn) | move;}
                    allMoves.add(new ChessBoard(ownPawns, ownKnights, ownBishops, ownRooks, ownQueens, ownKing, opponentPawns, opponentKnights, opponentBishops, opponentRooks, opponentQueens, opponentKing));
                    ownPawns = initialOwnPawns;
                    opponentPawns = chessBoard.getBP();
                    opponentKnights = chessBoard.getBN();
                    opponentBishops = chessBoard.getBB();
                    opponentRooks = chessBoard.getBR();
                    opponentQueens = chessBoard.getBQ();
                    opponentKing = chessBoard.getBK();
                    pawnMoves = (~move) & pawnMoves;
                }
                pawns = (~pawn) & pawns;
            }
        }

        return allMoves;
    }
}
