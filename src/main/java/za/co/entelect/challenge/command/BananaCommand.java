package za.co.entelect.challenge.command;

public class BananaCommand implements Command {

    private final int x;
    private final int y;
    private int selectedWorm;

    public BananaCommand(int x, int y) {
        this.x = x;
        this.y = y;
        this.selectedWorm = -1;
    }

    public BananaCommand(int x, int y, int select) {
        this.x = x;
        this.y = y;
        this.selectedWorm = select;
    }

    @Override
    public String render() {
        if (selectedWorm == -1) {
            return String.format("banana %d %d", x, y);
        }
        else {
            return String.format("select %d;banana %d %d", selectedWorm, x, y);
        }
    }
}
