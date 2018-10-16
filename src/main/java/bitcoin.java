import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.BriefLogFormatter;

public class bitcoin {
    public static void main(String args[]) {
        BriefLogFormatter.init();
        System.out.print(args.length);
        if (args.length < 2) {
            System.err.println("Usage: address-to-send-back-to [regtest|testnet]");
            return;
        }

        System.out.println("wtf");
        NetworkParameters params;
        String filePrefix;

        if (args[1].equals("testnet")) {
            params = TestNet3Params.get();
            filePrefix = "forwarding-service-testnet";
        } else if (args[1].equals("regtest")) {
            params = RegTestParams.get();
            filePrefix = "forwarding-service-regtest";
        } else {
            return;
        }
    }
}