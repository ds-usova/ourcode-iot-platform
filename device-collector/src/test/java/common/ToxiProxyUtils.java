package common;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.model.Toxic;
import lombok.SneakyThrows;

public class ToxiProxyUtils {

    private ToxiProxyUtils() {
    }

    @SneakyThrows
    public static void removeAllToxics(Proxy proxy) {
        proxy.toxics().getAll().forEach(ToxiProxyUtils::safeRemoveToxic);
    }

    @SneakyThrows
    private static void safeRemoveToxic(Toxic toxic) {
        toxic.remove();
    }

}
