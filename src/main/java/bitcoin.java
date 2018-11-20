import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class bitcoin {
    private static WalletAppKit kit;
    private static Address targetAddress;
    public static void main(String args[]) {
        BriefLogFormatter.init();
        Wallet wallet;
        NetworkParameters params = TestNet3Params.get();
        targetAddress = new Address(params, "miVBD7TZ6XVqy2mJNGM9D7xgVAgcrv48Ys");
        final File walletFile = new File("multisig.wallet");
        String filePrefix = "new";

        if (args[0].equals("createKey")) {
            ECKey key = new ECKey();

            try {
                wallet = new Wallet(params);
                wallet.importKey(key);
                wallet.saveToFile(walletFile);
                System.out.println("New wallet created");
            } catch (IOException e) {
                System.out.println("Unable to create wallet file.");
            }
        } else if (args[0].equals("checkKey")) {
            try {
                wallet = Wallet.loadFromFile(walletFile);
                ECKey key = wallet.currentReceiveKey();
                System.out.println("Address: " + key.toAddress(params));
                System.out.println("Public Key: " + key.getPublicKeyAsHex());
                System.out.println("Private Key: " + key.getPrivateKeyAsHex() + "\n");
                System.out.println(wallet);
            } catch (UnreadableWalletException e) {
                e.printStackTrace();
            }
        } else if (args[0].equals("receiveForward")) {
            kit = new WalletAppKit(params, new File("."), filePrefix) {
                @Override
                protected void onSetupCompleted() {
                    if (wallet().getKeyChainGroupSize() < 1)
                        wallet().importKey(new ECKey());

                    ECKey key = wallet().currentReceiveKey();
                    System.out.println("Address: " + key.toAddress(params));
                    System.out.println("Public Key: " + key.getPublicKeyAsHex());
                    System.out.println("Private Key: " + key.getPrivateKeyAsHex() + "\n");
                    System.out.println(wallet());
                }
            };

            // Download the block chain and wait until it's done.
            kit.startAsync();
            kit.awaitRunning();

            kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
                @Override
                public void onCoinsReceived(Wallet w, final Transaction tx, Coin prevBalance, Coin newBalance) {

                    Coin value = tx.getValueSentToMe(w);
                    System.out.println("Received tx for " + value.toFriendlyString() + ": " + tx);
                    System.out.println("Transaction will be forwarded after it confirms.");


                    Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
                        @Override
                        public void onSuccess(TransactionConfidence result) {
                            System.out.println("receive Success!");
                            forwardCoins(tx);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                        }

                    });
                }
            });
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException ignored) {}

        }

        /** For Multi-Signature, using bitcoind instead.
        /* You can find some reference here
        /* https://bitcoin.stackexchange.com/questions/6100/how-will-multisig-addresses-work */
//
//        else if (args[0].equals("multiSigSend")) {
//            filePrefix = "multisig";
//            System.out.println("Inside multiSig!");
//            kit = new WalletAppKit(params, new File("."), filePrefix);
//            kit.startAsync();
//            kit.awaitRunning();
//            System.out.println("Network connected!");
//            wallet = kit.wallet();
//            ECKey key = wallet.currentReceiveKey();
//            System.out.println("Address: " + key.toAddress(params));
//            System.out.println("Public Key: " + key.getPublicKeyAsHex());
//            System.out.println("Private Key: " + key.getPrivateKeyAsHex() + "\n");
//            System.out.println(wallet);
////            wallet.cleanup();
//            System.out.println(wallet);
////
//            try {
//                /* Sending Coins to Multisig */
//                /* Create MultisigAddress */
//                List<ECKey> keys = Arrays.asList(new ECKey(), new ECKey(), new ECKey(), new ECKey());
//                wallet.importKeys(keys);
//                /* Create Script */
//                Script payingToMultisigTxoutScript = ScriptBuilder.createMultiSigOutputScript(3, keys);
//                System.out.println("Is sent to multisig: " + payingToMultisigTxoutScript.isSentToMultiSig());
//                System.out.println("redeemScript: " + payingToMultisigTxoutScript);
//                /* Create Transaction */
//                Transaction payingToMultiSigTx = new Transaction(params);
//                Coin value = Coin.valueOf(0, 5);
//                payingToMultiSigTx.addOutput(value, payingToMultisigTxoutScript);
//                SendRequest request = SendRequest.forTx(payingToMultiSigTx);
//                wallet.completeTx(request);
//                PeerGroup peerGroup = kit.peerGroup();
//                peerGroup.broadcastTransaction(request.tx).broadcast().get();
//                System.out.println("Paying to multisig transaction broadcasted!");
//
//                Thread.sleep(Long.MAX_VALUE);
//            } catch(Exception e) {
//                e.printStackTrace();
//            }
//        } else if (args[0].equals("multiSigReceived")) {
////            filePrefix = "multisigReceived";
////            System.out.println("Inside multiSig!");
////            kit = new WalletAppKit(params, new File("."), filePrefix);
////            kit.startAsync();
////            kit.awaitRunning();
////            System.out.println("Network connected!");
////            wallet = kit.wallet();
////
////            Transaction redeemingMultisigTx1 = new Transaction(params);
////            TransactionOutput multisigOutput = wallet.getUnspents().get(0).getParentTransaction().getOutputs().stream().filter(unspent -> unspent.getScriptPubKey().isSentToMultiSig()).findFirst().get();
//
//
//        }
//

    }

    private static void forwardCoins(Transaction tx) {
        try {
            Coin value = tx.getValueSentToMe(kit.wallet());
            System.out.println("Forwarding " + value.toFriendlyString());
            final Coin amountToSend = value.subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
            System.out.println("Amount to send: " + value.toFriendlyString());
            final Wallet.SendResult sendResult = kit.wallet().sendCoins(kit.peerGroup(), targetAddress, amountToSend);
            System.out.println("sending...");
            sendResult.broadcastComplete.addListener(new Runnable() {
                @Override
                public void run() {
                    // The wallet has changed now, it'll get auto saved shortly or when the app shuts down.
                    System.out.println("Sent coins onwards! Transaction hash is " + sendResult.tx.getHashAsString());
                }
            }, MoreExecutors.directExecutor());
        } catch (InsufficientMoneyException e){
            e.printStackTrace();
        }
    }
}