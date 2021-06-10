import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static guru.nidi.graphviz.attribute.Rank.RankDir.LEFT_TO_RIGHT;
import static guru.nidi.graphviz.attribute.Records.rec;
import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;
import static guru.nidi.graphviz.model.Link.to;

class State {
    private int id;
    private State nextState1;
    private String operator1;
    private State nextState2;
    private String operator2;

    public State(int id) {
        this.id = id;
    }

    public State(int id, State nextState1, State nextState2) {
        this.id = id;
        this.nextState1 = nextState1;
        this.nextState2 = nextState2;
    }

    public State getNextState1() {
        return nextState1;
    }

    public void setNextState1(State nextState1) {
        this.nextState1 = nextState1;
    }

    public State getNextState2() {
        return nextState2;
    }

    public void setNextState2(State nextState2) {
        this.nextState2 = nextState2;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOperator1() {
        return operator1;
    }

    public void setOperator1(String operator1) {
        this.operator1 = operator1;
    }

    public String getOperator2() {
        return operator2;
    }

    public void setOperator2(String operator2) {
        this.operator2 = operator2;
    }
}

class FromTo {
    private State start;
    private State end;

    public FromTo() {
    }

    public FromTo(State start, State end) {
        this.start = start;
        this.end = end;
    }

    public State getStart() {
        return start;
    }

    public void setStart(State start) {
        this.start = start;
    }

    public State getEnd() {
        return end;
    }

    public void setEnd(State end) {
        this.end = end;
    }
}

class Bridge {
    int from;
    int to;
    String operator;

    public Bridge(int from, int to, String operator) {
        this.from = from;
        this.to = to;
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "Bridge{" +
                "from=" + from +
                ", to=" + to +
                ", operator='" + operator + '\'' +
                '}';
    }
}

public class Main {
    private static Stack<FromTo> fromToStack = new Stack<>();
    public static HashMap<Integer, HashSet<Integer>> e_closure = new HashMap<>();
    public static ArrayList<HashSet<Integer>> changedTable = new ArrayList<>();
    public static final String EPSILON = "ε";

    public static void main(String[] args) throws IOException {
        Scanner sin = new Scanner(System.in);
        StringBuffer regularExpression = new StringBuffer(sin.next());
        AddANDOperator(regularExpression);
        StringBuffer polandExpression = shuntingYardAlgorithm(regularExpression);
        System.out.println(polandExpression);
        toNFA(polandExpression);
        printE();
        getChangeTable();
        printChangeTable();
        drawDFA();
        System.out.println(DFAMap);
        minDFA();
    }

    public static Graph picMinDfa = graph("example").graphAttr().with(Rank.dir(LEFT_TO_RIGHT));
    public static HashMap<Integer, ArrayList<Integer>> minArray = new HashMap<>();
    public static HashMap<Integer, String> orderChar = new HashMap<>();

    private static void minDFA() throws IOException {
        int kk = 0;
        for (String c : charSet) {
            orderChar.put(kk++, c);
        }
        Integer end = fromToStack.peek().getEnd().getId();
        for (int i = 0; i < changedTable.size(); i++) {
            minArray.put(i, new ArrayList<>());
            for (String c : charSet) {
                boolean isFound = false;
                if (DFAMap.containsKey(i))
                    for (HashMap<String, Integer> k : DFAMap.get(i)) {
                        if (k.containsKey(c)) {
                            minArray.get(i).add(k.get(c));
                            isFound = true;
                            break;
                        }
                    }
                if (isFound == false) {
                    minArray.get(i).add(-1);
                }
            }
            if (changedTable.get(i).contains(end)) minArray.get(i).add(-1);
            else minArray.get(i).add(0);
        }
        System.out.println(minArray);
        int k = 10;
        while (k-- > 0) {
            boolean ok = false;
            for (Integer i : minArray.keySet()) {
                for (Integer j : minArray.keySet()) {
                    if (i.equals(j)) continue;
//                    if (minArray.get(i).toString().equals(minArray.get(j).toString())) {
//                        minArray.remove(j);
//                        exchange(j, i);
//                        ok=true;
//                    }
                    boolean isChange = true;
                    for (int l = 0; l < minArray.get(i).size(); l++) {
                        if ((minArray.get(i).get(l).equals(i) && minArray.get(j).get(l).equals(j)) || minArray.get(i).get(l).equals(minArray.get(j).get(l))) {
                            continue;
                        }
                        isChange = false;
                        break;
                    }
                    if (isChange) {
                        minArray.remove(j);
                        exchange(j, i);
                        ok = true;
                    }
                    if (ok) break;
                }
                if (ok) break;
            }
            System.out.println(k + ":" + minArray);
        }
        System.out.println(minArray);
        System.out.println(orderChar);
        for (Integer i : minArray.keySet()) {
            for (int j = 0; j < minArray.get(i).size() - 1; j++) {
                if (minArray.get(i).get(j) != -1) {
                    picMinDfa = picMinDfa.directed().with(node(String.valueOf(i)).link(to(node(String.valueOf(minArray.get(i).get(j)))).with(Label.of(orderChar.get(j)))));
                }
            }
        }
        Graphviz.fromGraph(picMinDfa) // 解析图形
                .width(1000) // 图片宽度
                .render(Format.PNG)  // 图片样式
                .toFile(new File("./PicMiniDFA.png")); // 输出图片
    }

    private static void exchange(int a, int b) {
        for (Integer i : minArray.keySet()) {
            for (int j = 0; j < minArray.get(i).size(); j++) {
                if (minArray.get(i).get(j) == a)
                    minArray.get(i).set(j, b);
            }
        }
    }

    public static HashMap<Integer, ArrayList<HashMap<String, Integer>>> DFAMap = new HashMap<>();
    public static Graph DFA = graph("DFA").graphAttr().with(Rank.dir(LEFT_TO_RIGHT));

    private static void drawDFA() throws IOException {
        for (Bridge bridge : bridgeSet) {
            HashMap<String, Integer> added = new HashMap<>();
            added.put(bridge.operator, bridge.to);
            if (!DFAMap.containsKey(bridge.from)) {
                ArrayList<HashMap<String, Integer>> relation = new ArrayList<>();
                relation.add(added);
                DFAMap.put(bridge.from, relation);
            } else DFAMap.get(bridge.from).add(added);
            DFA = DFA.directed().with(node(String.valueOf(bridge.from)).link(to(node(String.valueOf(bridge.to))).with(Label.of(bridge.operator))));
        }
        Graphviz.fromGraph(DFA) // 解析图形
                .width(1000) // 图片宽度
                .render(Format.PNG)  // 图片样式
                .toFile(new File("./PicDFA.png")); // 输出图片
    }

    private static void printChangeTable() {
        int i = 0;
        for (HashSet<Integer> set : changedTable) {
            System.out.print("changedTable(" + i++ + ")={ ");
            for (Integer integer : set) {
                System.out.print(integer + " ");
            }
            System.out.println("}");
        }
    }

    public static HashSet<Bridge> bridgeSet = new HashSet<>();
    public static HashSet<Integer> added;
    private static HashSet<Integer> closureSet = new HashSet<>();

    public static void getChangeTable() {
        int from = fromToStack.peek().getStart().getId();
        changedTable.add(e_closure.get(from));
        System.out.println(e_closure.get(from) + ":" + e_closure.get(from).toString().hashCode());
        closureSet.add(e_closure.get(from).toString().hashCode());
        for (int i = 0; i < changedTable.size(); i++) {
            for (String c : charSet) {
                added = new HashSet<>();
                set = new HashSet<>();
                for (int num : changedTable.get(i)) {
                    dfsNFA(stateSet.get(num), c);
                }
                System.out.println(added + " : " + added.toString().hashCode());
                if (added.size() != 0 && !closureSet.contains(added.toString().hashCode())) {
                    changedTable.add(added);
                    closureSet.add(added.toString().hashCode());
                }
                if (changedTable.contains(added)) {
                    Bridge bridge = new Bridge(i, changedTable.indexOf(added), c);
                    bridgeSet.add(bridge);
                }
            }
        }
    }

    public static HashSet<State> set;

    public static void dfsNFA(State start, String c) {
        set.add(start);
        if (start.getNextState1() != null && start.getOperator1().equals(c)) {
            added.add(start.getNextState1().getId());
            getClosureSet.clear();
            getClosure(start.getNextState1(), start.getNextState1().getId());
            if (!set.contains(start.getNextState1())) dfsNFA(start.getNextState1(), c);
        }
        if (start.getNextState2() != null && start.getOperator2().equals(c)) {
            added.add(start.getNextState2().getId());
            getClosureSet.clear();
            getClosure(start.getNextState2(), start.getNextState2().getId());
            if (!set.contains(start.getNextState2())) dfsNFA(start.getNextState2(), c);
        }
    }

    public static HashSet<State> getClosureSet = new HashSet<>();

    public static void getClosure(State now, int key) {
        getClosureSet.add(now);
        added.add(now.getId());
        if (now.getNextState1() != null && !getClosureSet.contains(now.getNextState1()) && now.getOperator1().equals(EPSILON)) {
            getClosure(now.getNextState1(), key);
        }
        if (now.getNextState2() != null && !getClosureSet.contains(now.getNextState2()) && now.getOperator2().equals(EPSILON)) {
            getClosure(now.getNextState2(), key);
        }
    }

    private static void printE() {
        for (int i = 0; i < e_closure.size(); i++) {
            System.out.print("e_closure(" + i + ") = { ");
            for (int num : e_closure.get(i)) {
                System.out.print(num + " ");
            }
            System.out.println("}");
        }
    }

    public static void addToNullEnd(State s, State add) {
        if (s.getNextState1() == null) {
            s.setNextState1(add);
            s.setOperator1(EPSILON);
        } else {
            s.setNextState2(add);
            s.setOperator2(EPSILON);
        }
    }

    public static Graph g = graph("example").graphAttr().with(Rank.dir(LEFT_TO_RIGHT));
    public static HashSet<String> charSet = new HashSet<>();

    public static void toNFA(StringBuffer s) throws IOException {
        int k = 0;
        int charNUM = 0;
        for (int i = 0; i < s.length(); i++) {
            char thisChar = s.charAt(i);
            switch (thisChar) {
                case '|':
                    FromTo ft1_1 = fromToStack.pop();
                    FromTo ft2_1 = fromToStack.pop();
                    State start_two = new State(k++);
                    start_two.setNextState1(ft1_1.getStart());
                    start_two.setOperator1(EPSILON);
                    start_two.setNextState2(ft2_1.getStart());
                    start_two.setOperator2(EPSILON);
                    State end_two = new State(k++);
                    addToNullEnd(ft1_1.getEnd(), end_two);
                    addToNullEnd(ft2_1.getEnd(), end_two);
                    fromToStack.push(new FromTo(start_two, end_two));
                    break;
                case '·':
                    FromTo ft2_2 = fromToStack.pop();
                    FromTo ft1_2 = fromToStack.pop();
                    addToNullEnd(ft1_2.getEnd(), ft2_2.getStart());
                    fromToStack.push(new FromTo(ft1_2.getStart(), ft2_2.getEnd()));
                    break;
                case '*':
                    State start_one = new State(k++);
                    State end_one = new State(k++);
                    FromTo ft_3 = fromToStack.pop();
                    addToNullEnd(ft_3.getEnd(), ft_3.getStart());
                    addToNullEnd(start_one, ft_3.getStart());
                    addToNullEnd(ft_3.getEnd(), end_one);
                    addToNullEnd(start_one, end_one);
                    fromToStack.push(new FromTo(start_one, end_one));
                    break;
                default:
                    State start = new State(k++);
                    State end = new State(k++);
                    start.setNextState1(end);
                    start.setOperator1(String.valueOf(thisChar));
                    charSet.add(String.valueOf(thisChar));
                    fromToStack.push(new FromTo(start, end));
                    break;
            }
        }
        drawNFA(fromToStack.peek().getStart());
        Graphviz.fromGraph(g) // 解析图形
                .width(1000) // 图片宽度
                .render(Format.PNG)  // 图片样式
                .toFile(new File("./PicNFA.png")); // 输出图片
    }


    public static HashSet<State> eClosureSet = new HashSet<>();

    public static void getEClosure(State now, int key) {
        e_closure.get(key).add(now.getId());
        eClosureSet.add(now);
        if (now.getNextState1() != null && !eClosureSet.contains(now.getNextState1()) && now.getOperator1().equals(EPSILON)) {
            getEClosure(now.getNextState1(), key);
        }
        if (now.getNextState2() != null && !eClosureSet.contains(now.getNextState2()) && now.getOperator2().equals(EPSILON)) {
            getEClosure(now.getNextState2(), key);
        }
    }

    public static HashMap<Integer, State> stateSet = new HashMap<>();

    public static void drawNFA(State start) {
        stateSet.put(start.getId(), start);
        HashSet<Integer> added = new HashSet<>();
        e_closure.put(start.getId(), added);
        eClosureSet.clear();
        getEClosure(start, start.getId());
        if (start.getNextState1() != null) {
            g = g.directed().with(node(String.valueOf(start.getId())).link(to(node(String.valueOf(start.getNextState1().getId()))).with(Label.of(start.getOperator1()))));
            if (!stateSet.containsKey(start.getNextState1().getId())) {
                drawNFA(start.getNextState1());
            }
        }
        if (start.getNextState2() != null) {
            g = g.directed().with(node(String.valueOf(start.getId())).link(to(node(String.valueOf(start.getNextState2().getId()))).with(Label.of(start.getOperator2()))));
            if (!stateSet.containsKey(start.getNextState2().getId())) {
                drawNFA(start.getNextState2());
            }
        }
    }

    public static StringBuffer shuntingYardAlgorithm(StringBuffer s) {
        HashMap<Character, Integer> m = new HashMap<>();
        m.put('*', 0);
        m.put('·', 1);
        m.put('|', 2);
        boolean[][] isPriority = {{true, true, true},
                {false, true, true},
                {false, false, true}};
        StringBuffer outcome = new StringBuffer();
        Stack<Character> operatorStack = new Stack<>();
        for (int i = 0; i < s.length(); i++) {
            char thisChar = s.charAt(i);
            if ((int) thisChar >= 'a' && (int) thisChar <= 'z')
                outcome.append(thisChar);
            else if (thisChar == '(') {
                operatorStack.push('(');
            } else if (thisChar == ')') {
                while (operatorStack.peek() != '(') {
                    outcome.append(operatorStack.pop());
                }
                operatorStack.pop();
            } else if (operatorStack.empty() || operatorStack.peek() == '(') {
                operatorStack.push(thisChar);
            } else {
                while (isPriority[m.get(operatorStack.peek())][m.get(thisChar)]) {
                    outcome.append(operatorStack.pop());
                    if (operatorStack.empty() || operatorStack.peek() == '(') break;
                }
                operatorStack.push(thisChar);
            }
        }
        while (!operatorStack.empty()) {
            outcome.append(operatorStack.pop());
        }
        return outcome;
    }

    public static void AddANDOperator(StringBuffer s) {
        String operatorLeft = "(|";
        String operatorRight = ")*|";
        for (int i = 0; i < s.length() - 1; i++) {
            if (operatorLeft.indexOf(s.charAt(i)) == -1 && operatorRight.indexOf(s.charAt(i + 1)) == -1) {
                s.insert(i + 1, '·');
                i += 1;
            }
        }
    }
}
