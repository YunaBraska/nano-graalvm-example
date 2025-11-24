package berlin.yuna.nativeexample;

import berlin.yuna.typemap.model.TypeMapI;
import org.nanonative.nano.core.model.Service;
import org.nanonative.nano.helper.event.model.Event;

public final class ExampleService extends Service {

    @Override
    public void stop() {

    }

    @Override
    public Object onFailure(final Event event) {
        return null;
    }

    @Override
    public void onEvent(final Event event) {

    }

    @Override
    public void configure(final TypeMapI<?> typeMapI, final TypeMapI<?> typeMapI1) {

    }

    @Override
    public void start() {
        context.info(() -> "[{}] started", name());
    }
}
