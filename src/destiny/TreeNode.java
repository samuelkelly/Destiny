package destiny;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static destiny.Board.*;

public class TreeNode
{
	public static void main(String[] args)
	{
		Board board = new Board(9);
		

		TreeNode treeNode = new TreeNode(board);
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input;

		try
		{
			while ((input = br.readLine()) != null)
			{
				if (input.equals("name"))
				{
					System.out.println("= Destiny\n");
				}
				else if (input.equals("protocol_version"))
				{
					System.out.println("= 2\n");
				}
				else if (input.equals("version"))
				{
					System.out.println("= 0.1\n");
				}
				else if (input.equals("list_commands"))
				{
					System.out.println("= genmove\nplay\nname\nprotocol_version\nversion\nwinrates\n");
				}
				else if (input.startsWith("boardsize"))
				{
					System.out.println("= \n");
				}
				else if (input.equals("clear_board"))
				{
					System.out.println("= \n");
				}
				else if (input.startsWith("komi"))
				{
					System.out.println("= \n");
				}
				else if (input.equals("winrates"))
				{
					treeNode.expand();
					System.out.print("=");
					System.out.println(" " + treeNode.board.pointToString(2) + " "
					+ (treeNode.children[2].wins + 0.0)
					/ treeNode.children[2].visits);
				}
				else if (input.startsWith("genmove"))
				{
					int move = -1;
					for (int i = 0; i < 2; i++)
					{
						for (int j = 0; j < 5000; j++)
							treeNode.singleIteration();
						move = treeNode.favoriteMove();
					}
					System.err.println(board.pointToString(move));
					System.err.println(treeNode.children[move].wins + "/" + treeNode.children[move].visits + "=" + (treeNode.children[move].wins + 0.0) / treeNode.children[move].visits);
					treeNode.chooseMove(move);
					System.out.println("= " + board.pointToString(move) + "\n");
					System.err.flush();
				}
				else if (input.startsWith("play"))
				{
					treeNode.expand();
					// System.err.println(input.substring(7));
					treeNode.chooseMove(board.stringToPoint(input.substring(7)));
					System.out.println("= \n");
				}
				else if (input.equals("showboard"))
				{
					System.out.println("= ");
					System.out.println(treeNode.board);
					System.out.println();
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public int generateMove(int playouts)
	{
		for (int i = 0; i < playouts; i++)
			singleIteration();
		int pt = favoriteMove();
		chooseMove(pt);
		return pt;
	}

	private void chooseMove(int move)
	{
		if (move == PASS)
		{
			// the board doesn't change
			board.play(PASS, true);
			visits = 0;
			wins = 0;
			children = null;
			expand();
		}
		else if (!board.isLegal(move))
		{
			throw new RuntimeException("Illegal move: " + board.pointToString(move));
		}
		else
		{
			// change to this part of the tree
			// System.err.println(move + " " + board.pointToString(move) + " " +
			// children[move] + " " + children.length);
			board = children[move].board;
			visits = children[move].visits;
			wins = children[move].wins;
			children = children[move].children;
//			if (board.isInAtari(move))
//				System.err.println(board.pointToString(board.soleLiberty(move)));
		}
	}

	/**
	 * The children of this node, or null if this node hasn't been expanded.
	 */
	private TreeNode[] children;

	/** How many playouts have been run through this node. */
	private int visits;

	/**
	 * How many playouts resulting in a win for the player have been run through
	 * this node.
	 */
	private int wins;

	/** The board associated with this node. */
	private Board board;

	/**
	 * A tiny value used to prevent division by 0 and to nudge UCT values to
	 * break ties.
	 */
	private static final double EPSILON = 1e-6;

	/** Our random number generator. */
	private static Random r = new Random();

	/** Makes a new TreeNode with the given board. */
	public TreeNode(Board board)
	{
		this.board = board;
	}

	/** Creates all legal children of this node. */
	public void expand()
	{
		children = new TreeNode[board.area()];
		for (int pt = 0; pt < board.area(); pt++)
		{
			Board copy = new Board(board);
			if (copy.play(pt, true))
				children[pt] = new TreeNode(copy);
			// otherwise it stays null
		}
	}

	/** @return the child with the highest UCT value, or null if no legal moves. */
	protected TreeNode childWithMaxUCT()
	{
		TreeNode favoriteSoFar = null;
		double bestUctValue = Double.MIN_VALUE;
		for (int pt : board.emptyPoints())
		{
			if (children[pt] == null)
				continue;
			double uctValue = uctValueOfChild(pt);
			if (uctValue > bestUctValue)
			{
				favoriteSoFar = children[pt];
				bestUctValue = uctValue;
			}
		}
		return favoriteSoFar;
	}

	/** @return the move with the most wins, or PASS if nothing else is legal. */
	private int favoriteMove()
	{
		int favoriteMove = PASS;
		int maxWins = -1;
		for (int pt : board.emptyPoints())
		{
			if (children[pt] == null)
				continue;
			int wins = children[pt].wins;
			if (wins > maxWins)
			{
				favoriteMove = pt;
				maxWins = wins;
			}
		}
		return favoriteMove;
	}

	/** Perform a single MCTS iteration, updating the tree. */
	protected void singleIteration()
	{
		// every node in the tree that we visit (including the one we add)
		List<TreeNode> visited = new LinkedList<TreeNode>();

		visited.add(this);

		// recursively select child with max UCT value until reaching a leaf
		TreeNode curr = this;
		while (curr.children != null && curr.childWithMaxUCT() != null)
		{
			curr = curr.childWithMaxUCT();
			visited.add(curr);
		}

		if (curr.visits > 4)
		{
			// expand this node and select the favorite child
			curr.expand();
			if (children != null && curr.childWithMaxUCT() != null)
			{
				curr = curr.childWithMaxUCT();
				visited.add(curr);
			}
		}

		// get the result of a single playout from this new child
		assert curr != null : "curr is null in singleIteration()";
		int winner = curr.board.winner();
		if (winner == EMPTY)
		{
			winner = curr.board.randomPlayout();
		}

		// back up the result to every node visited
		for (TreeNode n : visited)
			n.updateStats(winner);
	}
	
	public String toString(String indent)
	{
		String s = indent;
		s += wins + "/" + visits;
		if (children != null)
		{
			for (TreeNode child : children)
			{
				if (child != null)
					s += "\n" + child.toString(indent + "  ");
			}
		}
		return s;
	}

	public String toString()
	{
		return toString("");
	}

	protected double uctValueOfChild(int pt)
	{
		return (children[pt].wins + 1) / (children[pt].visits + 2 + EPSILON) + 0.2
		        * Math.sqrt(Math.log(visits + 1) / (children[pt].visits + EPSILON))
		        + r.nextDouble() * EPSILON;
	}

	protected void updateStats(int winner)
	{
		visits++;
		if (BLACK + WHITE - board.playerToMove == winner)
			wins++;
	}
}
