package com.example.chessapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.github.bhlangonijr.chesslib.Square

class MainActivity : AppCompatActivity() {
    private lateinit var chessBoardView: ChessBoardView
    private lateinit var chessEngine: ChessEngine
    private lateinit var btnNewGame: Button
    private lateinit var btnUndo: Button
    
    private var currentGameState: GameState? = null
    private var playerSide: Piece.Color = Piece.Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        chessBoardView = findViewById(R.id.chessBoard)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnUndo = findViewById(R.id.btnUndo)
        
        chessEngine = ChessEngine(this)
        chessEngine.initEngine()
        
        showSideSelectionDialog()
        
        chessBoardView.onPieceSelected = { position ->
            currentGameState?.let { state ->
                if (state.board[position]?.color == playerSide && state.currentPlayer == playerSide) {
                    val square = Square.values()[position]
                    val validSquares = chessEngine.getValidMoves(square).map { it.ordinal }
                    updateGameState(state.copy(
                        selectedPosition = position,
                        validMoves = validSquares
                    ))
                }
            }
        }
        
        chessBoardView.onMoveRequested = { from, to ->
            currentGameState?.let { state ->
                if (state.currentPlayer == playerSide) {
                    val fromSquare = Square.values()[from]
                    val toSquare = Square.values()[to]
                    if (chessEngine.makeMove(fromSquare, toSquare)) {
                        updateGameState(chessEngine.getBoardState())
                        // AI's turn
                        Handler(Looper.getMainLooper()).postDelayed({
                            chessEngine.makeAIMove()?.let {
                                updateGameState(chessEngine.getBoardState())
                            }
                        }, 500)
                    }
                }
            }
        }
        
        btnNewGame.setOnClickListener {
            showSideSelectionDialog()
        }
        
        btnUndo.setOnClickListener {
            // Implement undo functionality if needed
        }
    }
    
    private fun showSideSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_side_selection, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        dialogView.findViewById<Button>(R.id.btnWhite).setOnClickListener {
            playerSide = Piece.Color.WHITE
            chessEngine.setPlayerSide(playerSide)
            startNewGame()
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btnBlack).setOnClickListener {
            playerSide = Piece.Color.BLACK
            chessEngine.setPlayerSide(playerSide)
            startNewGame()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun startNewGame() {
        chessEngine.initEngine()
        updateGameState(chessEngine.getBoardState())
        
        if (playerSide == Piece.Color.BLACK) {
            // AI makes first move if player is black
            Handler(Looper.getMainLooper()).postDelayed({
                chessEngine.makeAIMove()?.let {
                    updateGameState(chessEngine.getBoardState())
                }
            }, 1000)
        }
    }
    
    private fun updateGameState(newState: GameState) {
        currentGameState = newState
        chessBoardView.updateGameState(newState)
        
        // Check for game end conditions
        if (newState.isCheckmate) {
            showGameEndDialog("Checkmate! ${newState.currentPlayer} wins!")
        } else if (newState.isStalemate) {
            showGameEndDialog("Stalemate! Game is a draw.")
        }
    }
    
    private fun showGameEndDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage(message)
            .setPositiveButton("New Game") { dialog, which ->
                showSideSelectionDialog()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        chessEngine.cleanup()
    }
}
