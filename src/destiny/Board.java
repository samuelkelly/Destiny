package destiny;

import java.util.ArrayList;
import java.util.Random;

/**
 * This class represents the whole game really: the board, whose turn it is, the
 * komi, etc.
 * 
 * It also has the ability to do a purely random playout and score it.
 */
public class Board
{
	public static void main(String[] args)
	{
		// we a bunch of games, for either profiling or benchmarking.
		// if you want to profile, run 10000000 games.
		// if you want to benchmark, run around 20000.

		long starttime = System.currentTimeMillis();
		double games;
		Board b = new Board(9);
		int blackWins = 0;
		int whiteWins = 0;
		for (games = 0; games < 20000; games++)
		{
			int winner = b.randomPlayout();
			if (winner == BLACK)
				blackWins++;
			else if (winner == WHITE)
				whiteWins++;
		}
		System.err.println(b);
		double seconds = (System.currentTimeMillis() - starttime) / 1000.0;
		System.out.println(games / seconds + "games/s; " + blackWins / games + " black wins/game; "
		        + whiteWins / games + " white wins/game.");
	}

	/** The width of the board. */
	private int width;

	/** The number of points on the board. */
	private int area;

	/** The color of an empty point. */
	public static final int EMPTY = 0;

	/** The color of a black stone. */
	public static final int BLACK = 1;

	/** The color of a white stone. */
	public static final int WHITE = 2;

	/** The color of the player who gets to move next. */
	int playerToMove;

	/** The color of each point on the board, EMPTY, BLACK, or WHITE. */
	private int[] color;

	/**
	 * If last move was a single-stone capture, this is the point captured;
	 * otherwise, it's -1.
	 */
	private int koPoint;

	/** The number of points of compensation to WHITE for going second. */
	private double komi;

	/**
	 * True if the last move was a pass (so if the next one is a pass, the game
	 * is over).
	 */
	private boolean lastMoveWasPass;

	/** True if the game is over (because both players passed). */
	private boolean gameIsOver;

	/**
	 * The index of the next stone in the chain, or -1 if not a stone. The chain
	 * is stored as singly linked cycle.
	 */
	private int[] next;

	/**
	 * The number of pseudo-liberties of the chain, if this is the chain head,
	 * or -1 if not a chain head.
	 */
	private int[] libCount;

	/** The sum of the ID's of all pseudo liberties. */
	private int[] libSum;

	/** The sum of the squares of all pseudo liberties. */
	private int[] libSquareSum;

	/**
	 * The adjacent points to each point on the board.
	 * 
	 * (Initialized upon construction.)
	 */
	private int[][] neighborhood;

	/**
	 * An array of vacant points for easily choosing a random move. Initially
	 * includes every point on the board and shrinks as more stones are placed.
	 */
	private ArrayList<Integer> emptyPoints;

	/** The value of a PASS move. */
	public static final int PASS = -1;

	/** A random number generator. */
	private Random r = new Random();

	/** Constructs a (deep) copy of the given Board. */
	public Board(Board that)
	{
		this.copyFrom(that);
	}

	/** Makes an empty Board of the given width. */
	public Board(int width)
	{
		this.koPoint = -1;
		this.komi = 7.5;
		this.playerToMove = BLACK;

		this.width = width;
		this.area = this.width * this.width;

		this.color = new int[this.area];
		this.next = new int[this.area];
		this.libCount = new int[this.area];
		this.libSum = new int[this.area];
		this.libSquareSum = new int[this.area];

		this.emptyPoints = new ArrayList<Integer>(area);
		for (int pt = 0; pt < area; pt++)
		{
			this.emptyPoints.add(pt);
		}

		for (int i = 0; i < area; i++)
		{
			this.color[i] = EMPTY;
			this.next[i] = -1;
			this.libCount[i] = -1;
		}

		neighborhood = new int[this.area][4];
		for (int p = 0; p < area; p++)
		{
			int r = pointRightOf(p);
			int d = pointBelow(p);
			int l = pointLeftOf(p);
			int u = pointAbove(p);
			neighborhood[p][0] = r;
			neighborhood[p][1] = d;
			neighborhood[p][2] = l;
			neighborhood[p][3] = u;
		}
	}

	public int area()
	{
		return area;
	}

	/**
	 * @return the point that is the head of the chain of the given point, or -1
	 *         if not a stone.
	 */
	protected int chainHead(int pt)
	{
		if (color[pt] == -1)
			return -1;
		while (libCount[pt] == -1)
			pt = next[pt];
		return pt;
	}

	/** @return the column index of the given point. */
	public int columnIndex(int pt)
	{
		return pt % width;
	}

	/** Connects the chains with the given heads into one chain. */
	protected void connect(int ptA, int ptB)
	{
		int t = next[ptA];
		next[ptA] = next[ptB];
		next[ptB] = t;
	}

	/** Makes this a (deep) copy of the given Board. */
	public void copyFrom(Board that)
	{
		// don't make a deep copy of this
		this.neighborhood = that.neighborhood;

		this.koPoint = that.koPoint;
		this.komi = that.komi;
		this.playerToMove = that.playerToMove;
		this.lastMoveWasPass = that.lastMoveWasPass;
		this.gameIsOver = that.gameIsOver;

		this.width = that.width;
		this.area = that.area;

		this.color = new int[this.area];
		this.next = new int[this.area];
		this.libCount = new int[this.area];
		this.libSum = new int[this.area];
		this.libSquareSum = new int[this.area];

		this.emptyPoints = new ArrayList<Integer>(that.emptyPoints);

		for (int i = 0; i < area; i++)
		{
			this.color[i] = that.color[i];
			this.next[i] = that.next[i];
			this.libCount[i] = that.libCount[i];
			this.libSum[i] = that.libSum[i];
			this.libSquareSum[i] = that.libSquareSum[i];
		}
	}

	public ArrayList<Integer> emptyPoints()
	{
		return emptyPoints;
	}

	public boolean gameIsOver()
	{
		return gameIsOver;
	}

	protected int immediateLibCount(int pt)
	{
		int libs = 0;
		for (int n : neighborhood[pt])
		{
			if (n != -1 && color[n] == EMPTY)
				libs++;
		}
		return libs;
	}

	protected int immediateLibSquareSum(int pt)
	{
		int sum = 0;
		for (int n : neighborhood[pt])
		{
			if (n != -1 && color[n] == EMPTY)
				sum += (n + 1) * (n + 1);
		}
		return sum;
	}

	protected int immediateLibSum(int pt)
	{
		int sum = 0;
		for (int n : neighborhood[pt])
		{
			if (n != -1 && color[n] == EMPTY)
				sum += (n + 1);
		}
		return sum;
	}

	/*
	 * Observation:
	 * 
	 * If a stone is in atari, it's libsum/libcount should be the id + 1 of the
	 * lib.
	 */

	public boolean isInAtari(int pt)
	{
		if (color[pt] == EMPTY)
			return false;
		pt = chainHead(pt);
		return libCount[pt] * libSquareSum[pt] == libSum[pt] * libSum[pt];
	}

	/**
	 * @return the liberty of the chain containing the given stone, if it is in
	 *         atari.
	 */
	public int soleLiberty(int pt)
	{
		assert isInAtari(pt);
		pt = chainHead(pt);
		return libSum[pt] / libCount[pt] - 1;
	}

	public boolean isLegal(int pt)
	{
		return play(pt, false);
	}

	public int offBoardNeighbors(int pt)
	{
		int offBoardNeighbors = 0;
		if (pointAbove(pt) == -1)
			offBoardNeighbors++;
		if (pointBelow(pt) == -1)
			offBoardNeighbors++;
		if (pointLeftOf(pt) == -1)
			offBoardNeighbors++;
		if (pointRightOf(pt) == -1)
			offBoardNeighbors++;
		return offBoardNeighbors;
	}

	public boolean isRealEye(int pt, int c)
	{
		// to be a real eye, all adj. neighbors must be same color
		return neighborsAreAll(pt, c);
	}

	public boolean neighborsAreAll(int p, int c)
	{
		return (pointAbove(p) == -1 || color[pointAbove(p)] == c)
		        && (pointBelow(p) == -1 || color[pointBelow(p)] == c)
		        && (pointLeftOf(p) == -1 || color[pointLeftOf(p)] == c)
		        && (pointRightOf(p) == -1 || color[pointRightOf(p)] == c);
	}

	public boolean play(int pt)
	{
		return play(pt, true);
	}

	public boolean play(int pt, boolean actuallyPlayIt)
	{
		// PASS is always legal
		if (pt == PASS)
		{
			if (actuallyPlayIt)
			{
				playerToMove = BLACK + WHITE - playerToMove;
				if (lastMoveWasPass)
					gameIsOver = true;
				lastMoveWasPass = true;
			}
			return true;
		}

		// playing on an occupied pt is always illegal
		if (pt == koPoint || color[pt] != EMPTY)
		{
			return false;
		}

		// is it suicide?
		boolean suicide = true;

		for (int n : neighborhood[pt])
		{
			if (n == -1)
				continue;
			else if (color[n] == EMPTY)
			{
				suicide = false;
				break;
			}

			int ch = chainHead(n);
			if (color[n] == playerToMove && !isInAtari(ch))
			{
				suicide = false;
				break;
			}
			if (color[n] == (BLACK + WHITE - playerToMove) && isInAtari(ch))
			{
				suicide = false;
				break;
			}
		}

		// suicide is always illegal
		if (suicide)
		{
			return false;
		}

		// the move is now legal
		if (!actuallyPlayIt)
			return true;

		// place the stone & remove its pt from the list of empty pts
		color[pt] = playerToMove;
		emptyPoints.remove(new Integer(pt));
		next[pt] = pt; // make it link to itself

		// count the liberty count, liberty sum, and liberty square sum
		libCount[pt] = immediateLibCount(pt);
		libSum[pt] = immediateLibSum(pt);
		libSquareSum[pt] = immediateLibSquareSum(pt);

		// decrease liberty counts of neighbors as a result of this stone's
		// placement
		for (int n : neighborhood[pt])
		{
			if (n != -1 && color[n] != EMPTY)
			{
				int ch = chainHead(n);
				libCount[ch]--;
				libSum[ch] -= (pt + 1);
				libSquareSum[ch] -= (pt + 1) * (pt + 1);
			}
		}

		// merge neighboring chains
		for (int n : neighborhood[pt])
		{
			if (n == -1 || color[n] != color[pt])
				continue; // skip enemies/empty points

			int ch = chainHead(n);

			if (ch == chainHead(pt))
				continue; // skip stones previously connected

			libSum[pt] += libSum[ch];
			libSum[ch] = 0;

			libSquareSum[pt] += libSquareSum[ch];
			libSquareSum[ch] = 0;

			libCount[pt] += libCount[ch];
			libCount[ch] = -1;

			connect(pt, n);
		}

		// process captures
		int stonesCaptured = 0;
		int pointCaptured = -1;
		for (int n : neighborhood[pt])
		{
			if (n != -1 && color[n] != color[pt] && color[n] != EMPTY
			        && libCount[chainHead(n)] == 0)
			{
				stonesCaptured += removeEntireChain(n);
				pointCaptured = n; // one of the pts captured
			}
		}

		if (stonesCaptured == 1)
			koPoint = pointCaptured;
		else
			koPoint = -1;

		playerToMove = BLACK + WHITE - playerToMove;
		return true;
	}

	/** Returns the point directly above p (or -1 if none exists). */
	public int pointAbove(int p)
	{
		int q = p - width;
		if (q < 0)
			return -1;
		else
			return q;
	}

	/** Returns the point directly below p (or -1 if none exists). */
	public int pointBelow(int p)
	{
		int q = p + width;
		if (q >= area)
			return -1;
		else
			return q;
	}

	public int pointFromRowAndColumn(int r, int c)
	{
		return r * width + c;
	}

	/** Returns the point directly left of p (or -1 if none exists). */
	public int pointLeftOf(int p)
	{
		int q = p - 1;
		if (rowIndex(q) != rowIndex(p))
			return -1;
		else
			return q;
	}

	/** Returns the point directly right of p (or -1 if none exists). */
	public int pointRightOf(int p)
	{
		int q = p + 1;
		if (rowIndex(q) != rowIndex(p))
			return -1;
		else
			return q;
	}

	// now i'm gonna try to change the policy to always try to capture a chain
	// in atari

	// how?

	// make a list of liberties of chains in atari

	public ArrayList<Integer> capturePoints()
	{
		ArrayList<Integer> capturePoints = new ArrayList<Integer>();
		for (int pt = 0; pt < area(); pt++)
			if (color[pt] == BLACK + WHITE - playerToMove && isInAtari(pt))
				capturePoints.add(soleLiberty(pt));
		return capturePoints;
	}

	/**
	 * Executes a random playout from the current position and returns the
	 * winner, or EMPTY if there is a tie.
	 * 
	 * Also, adds all points played by the winner
	 */
	public int randomPlayout()
	{
		Board copy = new Board(this);

		while (!copy.gameIsOver)
		{
			ArrayList<Integer> legalMoves = new ArrayList<Integer>(copy.emptyPoints);
			do
			{
				int randomPt = legalMoves.get(r.nextInt(legalMoves.size()));

				if (!copy.isRealEye(randomPt, playerToMove) && copy.play(randomPt, true))
					break;
				else
					legalMoves.remove(new Integer(randomPt));
			}
			while (legalMoves.size() > 0);

			if (legalMoves.size() == 0)
				copy.play(PASS, true);
		}

		return copy.winner();
	}

	/**
	 * Remove an entire chain from the board because of capture. This adds
	 * liberties to surrounding chains.
	 * 
	 * @param pt
	 *            The location of some stone in the chain.
	 */
	protected int removeEntireChain(int pt)
	{
		int x = pt;
		int chainColor = color[pt];
		int count = 0;

		while (color[x] != EMPTY)
		{
			// save the location of the next stone in the chain
			int t = next[x];

			// remove the current stone
			color[x] = EMPTY;
			emptyPoints.add(x);
			count++;
			next[x] = -1;
			libCount[x] = -1;

			// increase liberty count of all enemy neighbors
			for (int n : neighborhood[x])
			{
				if (n != -1 && color[n] != EMPTY && color[n] != chainColor)
				{
					int ch = chainHead(n);
					libCount[ch]++;
					libSum[ch] += (x + 1);
					libSquareSum[ch] += (x + 1) * (x + 1);
				}
			}

			// prepare to do the same on the next stone
			x = t;
		}

		return count;
	}

	/** @return the row index of the given point. */
	public int rowIndex(int pt)
	{
		return width - 1 - pt / width;
	}

	/** Returns the score of a fully played out game for player. */
	public double score(int c)
	{
		if (c == EMPTY)
			throw new RuntimeException("Invalid color: EMPTY!");

		int score = 0;
		for (int p = 0; p < area; p++)
		{
			if (color[p] == c)
				score++;
			else if (color[p] == EMPTY && neighborsAreAll(p, c))
				score++;
		}

		if (c == BLACK)
			return score - komi / 2.0;
		else
			return score + komi / 2.0;
	}

	public char letterCoordinate(int columnIndex)
	{
		char letter = (char) ((int) ('A') + columnIndex);
		if (letter > 'H')
			letter += 1;
		return letter;
	}

	public int columnIndex(char letter)
	{
		int index = letter - 'A';
		if (letter > 'H')
			index -= 1;
		return index;
	}

	public String pointToString(int pt)
	{
		if (pt == PASS)
			return "PASS";
		String s = "" + letterCoordinate(columnIndex(pt)) + (rowIndex(pt) + 1);
		return s;
	}

	public int stringToPoint(String s)
	{
		if (s.equals("PASS"))
			return PASS;
		char letter = s.charAt(0);
		int colIndex = letter - 'A';
		if (letter > 'H')
			colIndex--;
		int rowIndex = width() - Integer.parseInt(s.substring(1));
		// rowIndex--;
		return pointFromRowAndColumn(rowIndex, colIndex);
	}

	/**
	 * @return a human-readable textual representation of the board, with
	 *         coordinates.
	 */
	public String toString()
	{
		String s = "";

		// the letter coordinates of the columns
		String letterCoordinates = "";
		for (int c = 0; c < width; c++)
		{
			letterCoordinates += letterCoordinate(c) + " ";
		}

		s += "   " + letterCoordinates + "\n";
		for (int r = 0; r < width; r++)
		{
			s += String.format("%2d ", width - r);
			for (int c = 0; c < width; c++)
			{
				int pt = pointFromRowAndColumn(r, c);
				switch (color[pt])
				{
				case EMPTY:
					s += ". ";
					break;
				case BLACK:
					s += "X ";
					break;
				case WHITE:
					s += "O ";
					break;
				}
			}
			s += String.format("%2d", width - r) + "\n";
		}
		s += "   " + letterCoordinates + "\n";
		return s;
	}

	public int width()
	{
		return width;
	}

	/** If the playout is over, returns the winner. Otherwise returns VACANT. */
	public int winner()
	{
		if (gameIsOver)
			return score(WHITE) > score(BLACK) ? WHITE : BLACK;
		else
			return EMPTY;
	}

}