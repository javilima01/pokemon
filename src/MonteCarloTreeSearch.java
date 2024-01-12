import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MonteCarloTreeSearch {
    private static final int SIMULATION_COUNT = 1000;
    private static final double EXPLORATION_PARAMETER = 1.4;
    private static final double MAX_SCORE = 33.33 * 3 + 100;
    private static final double MIN_SCORE = 0;
    /**
     * Find the best move using Monte Carlo Tree Search.
     * @param initialState The initial state of the game.
     * @return The best move.
     */
    public int findBestMove(PokemonBattleState initialState) {
        Node root = new Node(new PokemonBattleState(initialState));
        // Run simulations to build the tree
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            Node selectedNode = select(root);
            Node expandedNode = expand(selectedNode);
            double simulationResult = simulate(expandedNode);
            backpropagate(expandedNode, simulationResult);
        }

        // Choose the best move based on the tree
        Node bestChild = getBestChild(root);
        return bestChild.action;
    }

    /**
     * Select a node for exploration in the tree.
     * @param node The current node.
     * @return The selected node.
     */
    private Node select(Node node) {
        // Traverse the tree until a node is found that is not fully expanded or is terminal
        Node nodeAux = node;
        while (nodeAux.children.size() > 0) {
            for (int i = 0; i < nodeAux.children.size(); i++){
                if (nodeAux.children.get(i).visits == 0) {
                    return expand(nodeAux);
                }
            }
            nodeAux = nodeAux.bestChild(EXPLORATION_PARAMETER);
            if (!nodeAux.state.isTerminal() && node.getUntriedActions().isEmpty()) {
                return nodeAux; // Explicitly expand if the best child has not been expanded
            }
        }
        return node;
    }

    /**
     * Expand the tree by adding a new child node.
     * @param node The current node.
     * @return The expanded node.
     */
    private Node expand(Node node) {
        List<Integer> untriedActions = node.getUntriedActions();
        if (!untriedActions.isEmpty() && !node.state.isTerminal()) {
            int randomAction = untriedActions.remove(new Random().nextInt(untriedActions.size()));
            PokemonBattleState nextState = node.state.performAction(randomAction);
            Node child = new Node(nextState, node, randomAction);
            node.children.add(child);
            return child;
        }
        return node;
    }

    /**
     * Simulate a random playthrough from a node.
     * @param node The node to start the simulation from.
     * @return The result of the simulation.
     */
    private double simulate(Node node) {
        PokemonBattleState state = node.state;
        while (!state.isTerminal()) {
            List<Integer> legalActions = state.getLegalActions();
            // int nextAction = state.getAction();
            int nextAction = new Random().nextInt(legalActions.size());
            int randomAction = legalActions.get(nextAction);
            state = state.performAction(randomAction);
        }
        if (node.action != -1 && state.getScore(node.state) > 0) {
            return state.getScore(node.state) * node.state.getPlayer2().getCurrentPokemon().getMoves()[node.action].getAccuracy()/100.0;
        } else if (node.action != -1 && state.getScore(node.state) < 0) {
            double accuracy = node.state.getPlayer2().getCurrentPokemon().getMoves()[node.action].getAccuracy()/100.0;
            return state.getScore(node.state) * (1 + 1 - accuracy) ;
        }
        return state.getScore(node.state);
    }

    /**
     * Backpropagate the result of a simulation up the tree.
     * @param node The node to start the backpropagation from.
     * @param result The result of the simulation.
     */
    private void backpropagate(Node node, double result) {
        while (node != null) {
            node.visits++;
            node.totalScore += result;
            node = node.parent;
        }
    }

    /**
     * Get the best child node based on exploration and exploitation.
     * @param node The current node.
     * @return The best child node.
     */
    private Node getBestChild(Node node) {
        return node.bestChild(0); // Exploration parameter set to 0 for pure exploitation
    }

    /**
     * Internal class representing a node in the MCTS tree.
     */
    private class Node {
        PokemonBattleState state;
        Node parent;
        int action;
        int visits;
        double totalScore;
        List<Node> children;

        Node(PokemonBattleState state) {
            this.totalScore = 0;
            this.visits = 0;
            this.parent = null;
            this.state = state;
            this.children = new ArrayList<>();
            this.action = -1;
        }

        Node(PokemonBattleState state, int action) {
            this(state);
            this.action = action;
        }

        Node(PokemonBattleState state, Node parent, int action) {
            this(state, action);
            this.parent = parent;
        }
        /**
         * Check if the node is fully expanded.
         * @return True if fully expanded, false otherwise.
         */
        boolean isFullyExpanded() {
            return getUntriedActions().isEmpty();
        }

        /**
         * Get untried actions from the current state.
         * @return List of untried actions.
         */
        List<Integer> getUntriedActions() {
            List<Integer> allActions = state.getLegalActions();
            List<Integer> triedActions = new ArrayList<>();
            for (Node child : children) {
                triedActions.add(child.action);
            }
            allActions.removeAll(triedActions);
            return allActions;
        }

        /**
         * Get the best child node based on exploration and exploitation.
         * @param explorationParameter The exploration parameter.
         * @return The best child node.
         */
        Node bestChild(double explorationParameter) {
            double bestValue = Double.NEGATIVE_INFINITY;
            Node bestChild = null;
            for (Node child : children) {
                double normalizedScore = (child.totalScore - MIN_SCORE) / (MAX_SCORE - MIN_SCORE + 1.0);
                // Calculate exploitation and exploration terms
                double exploitation = (normalizedScore / child.visits);
                double exploration = Math.sqrt(Math.log(visits) / child.visits);
                double value = exploitation + exploration + Math.random() * explorationParameter/5;
                if (value > bestValue) {
                    bestValue = value;
                    bestChild = child;
                }
            }
            return bestChild;
        }
    }
}