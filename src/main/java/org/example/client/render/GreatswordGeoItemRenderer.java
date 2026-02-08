package org.example.client.render;

import org.example.item.GreatswordGeoItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GreatswordGeoItemRenderer extends GeoItemRenderer<GreatswordGeoItem> {
    public GreatswordGeoItemRenderer() {
        super(new GreatswordGeoModel());
    }
}
