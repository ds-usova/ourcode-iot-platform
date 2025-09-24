package common;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.model.Toxic;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import lombok.SneakyThrows;

import java.io.IOException;

public class ToxiProxyUtils {

    private ToxiProxyUtils() {
    }

    @SneakyThrows
    public static void removeAllToxics(Proxy proxy) {
        proxy.toxics().getAll().forEach(ToxiProxyUtils::safeRemoveToxic);
    }

    public static void cutConnection(Proxy proxy) throws IOException {
        proxy.toxics().limitData("cut_connection", ToxicDirection.DOWNSTREAM, 0);
    }

    @SneakyThrows
    private static void safeRemoveToxic(Toxic toxic) {
        toxic.remove();
    }

}
