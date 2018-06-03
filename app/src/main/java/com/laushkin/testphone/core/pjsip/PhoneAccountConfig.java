package com.laushkin.testphone.core.pjsip;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.ContainerNode;

/**
 * @author Vasily Laushkin <vaslinux@gmail.com> on 27/05/2018.
 */
public class PhoneAccountConfig {
    public AccountConfig accCfg = new AccountConfig();

    public void readObject(ContainerNode node) {
        try {
            ContainerNode acc_node = node.readContainer("Account");
            accCfg.readObject(acc_node);
        } catch (Exception e) {

        }
    }

    public void writeObject(ContainerNode node) {
        try {
            ContainerNode acc_node = node.writeNewContainer("Account");
            accCfg.writeObject(acc_node);
        } catch (Exception e) {

        }
    }
}
