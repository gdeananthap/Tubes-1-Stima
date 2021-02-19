package za.co.entelect.challenge.command;

public class SnowballCommand implements Command {

    private final int x;
    private final int y;
    private int selectedWorm;

    public SnowballCommand(int x, int y) {
        this.x = x;
        this.y = y;
        this.selectedWorm = -1;
    }

    public SnowballCommand(int x, int y, int select) {
        this.x = x;
        this.y = y;
        this.selectedWorm = select;
    }

    @Override
    public String render() {
        if (selectedWorm == -1) {
            return String.format("snowball %d %d", x, y);
        }
        else {
            return String.format("select %d;snowball %d %d", selectedWorm, x, y);
        }
    }
}
