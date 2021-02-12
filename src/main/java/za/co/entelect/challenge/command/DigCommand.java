package za.co.entelect.challenge.command;

public class DigCommand implements Command {

    private final int x;
    private final int y;
    private int selectedWorm;

    public DigCommand(int x, int y) {
        this.x = x;
        this.y = y;
        this.selectedWorm = -1;
    }

    public DigCommand(int x, int y, int select) {
        this.x = x;
        this.y = y;
        this.selectedWorm = select;
    }

    @Override
    public String render() {
        if (selectedWorm == -1) {
            return String.format("dig %d %d", x, y);
        }
        else {
            return String.format("select %d;dig %d %d", selectedWorm, x, y);
        }
    }
}
