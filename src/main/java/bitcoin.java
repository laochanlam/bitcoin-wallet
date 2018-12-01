import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import static com.google.common.base.Preconditions.checkState;


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
        String filePrefix = "multisig";

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
        } else if (args[0].equals("checkWallet")) {
                kit = new WalletAppKit(params, new File("."), filePrefix);
                kit.startAsync();
                kit.awaitRunning();
                wallet = kit.wallet();
                ECKey key = wallet.currentReceiveKey();
                System.out.println("Address: " + key.toAddress(params));
                System.out.println("Public Key: " + key.getPublicKeyAsHex());
                System.out.println("Private Key: " + key.getPrivateKeyAsHex() + "\n");
                System.out.println(wallet);
                /* For debug printing
                List <ECKey> keys = wallet.getImportedKeys();
                System.out.println(keys.get(0).getPublicKeyAsHex());
                System.out.println(keys.get(0).getPrivateKeyAsHex());
                System.out.println(keys.get(1).getPublicKeyAsHex());
                System.out.println(keys.get(1).getPrivateKeyAsHex());
                System.out.println(keys.get(2).getPublicKeyAsHex());
                System.out.println(keys.get(2).getPrivateKeyAsHex());
                System.out.println(keys.get(3).getPublicKeyAsHex());
                System.out.println(keys.get(3).getPrivateKeyAsHex());
                */
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
                // wait for the listener
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException ignored) {}

        }
        else if (args[0].equals("newMultiSigWallet")) {
            filePrefix = "multisig";
            kit = new WalletAppKit(params, new File("."), filePrefix);
            kit.startAsync();
            kit.awaitRunning();
            wallet = kit.wallet();
            System.out.println("Generating 4 Keys");
            /* Create MultisigAddress */
            List<ECKey> keys = Arrays.asList(new ECKey(), new ECKey(), new ECKey(), new ECKey());
            wallet.importKeys(keys);
            System.out.println(wallet);
            System.out.println(wallet.currentReceiveAddress());
        }
        else if (args[0].equals("SendtoMultisig")) {
            filePrefix = "multisig";
            kit = new WalletAppKit(params, new File("."), filePrefix);
            kit.startAsync();
            kit.awaitRunning();
            wallet = kit.wallet();
            System.out.println("Network connected!");
            List <ECKey> keys = wallet.getImportedKeys();

            System.out.println(wallet);
            System.out.println(keys);

            try {
                /* Sending Coins to Multisig */
                /* Create Script */
                Script payingToMultisigTxoutScript = ScriptBuilder.createMultiSigOutputScript(3, keys);
                System.out.println("Is sent to multisig: " + payingToMultisigTxoutScript.isSentToMultiSig());
                System.out.println("redeemScript: " + payingToMultisigTxoutScript);
                /* Create Transaction */
                Transaction payingToMultiSigTx = new Transaction(params);
                Coin value = Coin.valueOf(0,3);
                payingToMultiSigTx.addOutput(value, payingToMultisigTxoutScript);
                SendRequest request = SendRequest.forTx(payingToMultiSigTx);
                wallet.completeTx(request);
                PeerGroup peerGroup = kit.peerGroup();
                peerGroup.broadcastTransaction(request.tx).broadcast().get();
                System.out.println("Paying to multisig transaction broadcasted!");

                Thread.sleep(Long.MAX_VALUE);
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else if (args[0].equals("SendfromMultiSig")) {
            filePrefix = "multisig";
            kit = new WalletAppKit(params, new File("."), filePrefix);
            kit.startAsync();
            kit.awaitRunning();
            wallet = kit.wallet();
            System.out.println("Network connected!");
            List <ECKey> keys = wallet.getImportedKeys();

            System.out.println(wallet);
            System.out.println(keys);

            // finding script
            Transaction redeemingMultisigTx1 = new Transaction(params);
            Transaction redeemingMultisigTx2 = new Transaction(params);
            Transaction redeemingMultisigTx3 = new Transaction(params);
            TransactionOutput multisigOutput = wallet.getUnspents().get(0).getParentTransaction().getOutputs().get(1);
            System.out.println(multisigOutput);
            redeemingMultisigTx1.addInput(multisigOutput);
            redeemingMultisigTx2.addInput(multisigOutput);
            TransactionInput redeemMultisigTxInput = redeemingMultisigTx3.addInput(multisigOutput);

            // output script
            Coin value = multisigOutput.getValue();
            Address finalAddress = targetAddress;
            System.out.println("Value:" + value);
            System.out.println("Value:" + value.div(2));
            System.out.println("currentReceiveAddress:" + finalAddress);
            redeemingMultisigTx1.addOutput(value.div(2), finalAddress);
            redeemingMultisigTx2.addOutput(value.div(2), finalAddress);
            redeemingMultisigTx3.addOutput(value.div(2), finalAddress);

            // hashing
            Script payingToMultisigTxoutScriptPubKey = multisigOutput.getScriptPubKey();
            System.out.println("payingToMultisigTxoutScriptPubKey: " + payingToMultisigTxoutScriptPubKey);
            checkState(payingToMultisigTxoutScriptPubKey.isSentToMultiSig());
            Sha256Hash sighash1 = redeemingMultisigTx1.hashForSignature(0, payingToMultisigTxoutScriptPubKey, Transaction.SigHash.ALL, false);
            Sha256Hash sighash2 = redeemingMultisigTx2.hashForSignature(0, payingToMultisigTxoutScriptPubKey, Transaction.SigHash.ALL, false);
            Sha256Hash sighash3 = redeemingMultisigTx3.hashForSignature(0, payingToMultisigTxoutScriptPubKey, Transaction.SigHash.ALL, false);

            // sign
            ECKey.ECDSASignature partyASignature = wallet.getImportedKeys().get(0).sign(sighash1);
            ECKey.ECDSASignature partyBSignature = wallet.getImportedKeys().get(1).sign(sighash2);
            ECKey.ECDSASignature partyCSignature = wallet.getImportedKeys().get(2).sign(sighash3);

            TransactionSignature signatureA = new TransactionSignature(partyASignature, Transaction.SigHash.ALL, false);
            TransactionSignature signatureB = new TransactionSignature(partyBSignature, Transaction.SigHash.ALL, false);
            TransactionSignature signatureC = new TransactionSignature(partyCSignature, Transaction.SigHash.ALL, false);
            Script inputScript = ScriptBuilder.createMultiSigInputScript(signatureA, signatureB, signatureC);
            System.out.println("redeeming Tx input script: " + inputScript);
            redeemMultisigTxInput.setScriptSig(inputScript);
            System.out.println("End of Setting");
            redeemMultisigTxInput.verify(multisigOutput);
            System.out.println("End of verifying");
            PeerGroup peerGroup = kit.peerGroup();
            System.out.println("End of getting peer");
            try {
                peerGroup.broadcastTransaction(redeemingMultisigTx3).broadcast().get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Multisig redeeming transaction broadcasted!");
        }
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