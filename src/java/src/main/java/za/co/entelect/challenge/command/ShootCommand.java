package za.co.entelect.challenge.command;

import za.co.entelect.challenge.enums.Direction;

public class ShootCommand implements Command {

    private Direction direction;
    private int selectedWorm;

    public ShootCommand(Direction direction) {
        this.direction = direction;
        this.selectedWorm = -1;
    }

    public ShootCommand(Direction direction, int select) {
        this.direction = direction;
        this.selectedWorm = select;
    }

    @Override
    public String render() {
        if (selectedWorm == -1) {
            return String.format("shoot %s", direction.name());
        }
        else {
            return String.format("select %d;shoot %s", selectedWorm, direction.name());
        }
    }
}
