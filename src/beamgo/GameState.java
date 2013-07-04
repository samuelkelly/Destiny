package beamgo;

/** The state of the game. */
public class GameState
{
	private int[] board;
	private final int boardWidth;
	private int playerJustMoved;
	public static final int BLACK = 1;
	public static final int WHITE = 2;
	
	/** The default constructor. */
	public GameState()
	{
		boardWidth = 19;
		board = new int[boardWidth * boardWidth];
		playerJustMoved = WHITE;
	}
	
	/** The copy constructor. */
	public GameState(GameState that)
	{
		if (this.boardWidth != that.boardWidth)
			throw new RuntimeException("Incompatible size.");
		this.boardWidth = that.boardWidth;
		this.playerJustMoved = that.playerJustMoved;
		for (int p = 0; p < boardWidth * boardWidth; p++)
			this.board[p] = that.board[p];
	}
	
	@Override
	public String toString()
	{
		// the row of letters "   A B C ... H J K ... T"
		String letterRow = "   ";
		for (int i = 0; i < boardWidth; i++)
		{
			char letter = (char)((int) ('A') + i);
			if (letter > 'H')
				letter++;
			letterRow += letter + " ";
		}
		letterRow += "\n";
		
		// the board
		String s = letterRow;
		for (int r = 0; r < boardWidth; r++)
		{
			// the row number, right-aligned
			s += String.format("%2s ", (19 - r));
			
			// the row itself
			for (int c = 0; c < boardWidth; c++)
			{
				switch (board[r * boardWidth + c])
				{
				case BLACK:
					s += "# ";
					break;
				case WHITE:
					s += "O ";
					break;
				default:
					s += ". ";
				}
			}
			
			// the row number, left-aligned
			s += 19 - r;
			s += "\n";
		}
		s += letterRow;
		return s;
	}
	
	public void play(int p)
	{
		board[p] = BLACK + WHITE - playerJustMoved;
		playerJustMoved = BLACK + WHITE - playerJustMoved;
	}
	
	public static void main(String[] args)
	{
		GameState g = new GameState();
		System.out.println(g);
		g.play(34);
		g.play(35);
		System.out.println(g);
	}	
}
