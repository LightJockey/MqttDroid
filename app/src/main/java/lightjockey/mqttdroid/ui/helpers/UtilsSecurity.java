package lightjockey.mqttdroid.ui.helpers;

import android.annotation.SuppressLint;

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;

public class UtilsSecurity {
    // https://github.com/hivemq/hivemq-mqtt-client/issues/595
    public static TrustManagerFactory trustAllCertsFactory() {
        return new TrustAllCertsFactory();
    }

    private static final class TrustAllCertsFactory extends TrustManagerFactory {
        private TrustAllCertsFactory() {
            super(new TrustAllCertsFactorySpi(), TrustManagerAllCerts.TMF_DEF.getProvider(), TrustManagerAllCerts.TMF_DEF.getAlgorithm());
        }
    }

    private static class TrustAllCertsFactorySpi extends TrustManagerFactorySpi {
        TrustAllCertsFactorySpi() {
        }

        @Override
        protected void engineInit(KeyStore ks) {
        }
        @Override
        protected void engineInit(ManagerFactoryParameters spec) {
        }
        @Override
        protected TrustManager[] engineGetTrustManagers() {
            return new TrustManager[]{new TrustManagerAllCerts()};
        }
    }

    @SuppressLint("CustomX509TrustManager")
    private static class TrustManagerAllCerts extends X509ExtendedTrustManager {
        TrustManagerAllCerts() {
        }

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        private final static TrustManagerFactory TMF_DEF = createTmfDefault();

        private static TrustManagerFactory createTmfDefault() {
            try {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore)null);  // initialises the TMF with the default trust store
                return tmf;
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
