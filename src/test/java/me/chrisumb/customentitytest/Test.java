package me.chrisumb.customentitytest;

import com.mysql.cj.result.Field;
import me.chrisumb.customentity.Skin;

import java.io.File;

public class Test {

    public static void main(String[] args) {

        Skin skin = Skin.download("Tom1024");
        skin.save(new File("tom1024.skin"));

        Skin load = Skin.load(new File("tom1024.skin"));

        System.out.println(skin.getValue().equals(load.getValue()) && skin.getSignature().equals(load.getSignature()));
    }
}
