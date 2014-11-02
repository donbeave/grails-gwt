package @artifact.package@;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

public class @artifact.name@Event extends GwtEvent<@artifact.name@Handler> {

    public static final Type<@artifact.name@Handler> TYPE = new Type<@artifact.name@Handler>();

    public static void fire(HasHandlers source) {
        source.fireEvent(new @artifact.name@Event());
    }

    public static void fire(HasHandlers source, @artifact.name@Event eventInstance) {
        source.fireEvent(eventInstance);
    }

    public static Type<@artifact.name@Handler> getType() {
        return TYPE;
    }

    public @artifact.name@Event() {
        // Possibly for serialization.
    }

    @Override
    public Type<@artifact.name@Handler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(@artifact.name@Handler handler) {
        handler.onEvent(this);
    }

}
