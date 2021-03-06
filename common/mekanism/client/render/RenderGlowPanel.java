package mekanism.client.render;

import java.util.HashMap;
import java.util.Map;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.client.model.ModelTransporterBox;
import mekanism.client.render.MekanismRenderer.DisplayInteger;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.multipart.PartDiversionTransporter;
import mekanism.common.multipart.PartGlowPanel;
import mekanism.common.multipart.PartLogisticalTransporter;
import mekanism.common.multipart.PartMechanicalPipe;
import mekanism.common.multipart.PartPressurizedTube;
import mekanism.common.multipart.PartSidedPipe;
import mekanism.common.multipart.PartSidedPipe.ConnectionType;
import mekanism.common.multipart.PartTransmitter;
import mekanism.common.multipart.PartUniversalCable;
import mekanism.common.multipart.TransmitterType;
import mekanism.common.multipart.TransmitterType.Size;
import mekanism.common.transporter.TransporterStack;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.TransporterUtils;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import org.lwjgl.opengl.GL11;

import codechicken.lib.colour.Colour;
import codechicken.lib.colour.ColourRGBA;
import codechicken.lib.lighting.LazyLightMatrix;
import codechicken.lib.lighting.LightMatrix;
import codechicken.lib.lighting.LightModel;
import codechicken.lib.lighting.LightModel.Light;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.ColourMultiplier;
import codechicken.lib.render.IVertexModifier;
import codechicken.lib.render.IconTransformation;
import codechicken.lib.render.TextureUtils;
import codechicken.lib.render.TextureUtils.IIconRegister;
import codechicken.lib.render.UV;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;

public class RenderGlowPanel implements IIconRegister
{
	public static RenderGlowPanel INSTANCE;

	public static CCModel[] frameModels;
	public static CCModel[] lightModels;

	public static Icon icon;

	public static RenderGlowPanel getInstance()
	{
		return INSTANCE;
	}

	public static void init()
	{
		INSTANCE = new RenderGlowPanel();
		TextureUtils.addIconRegistrar(INSTANCE);

		Map<String, CCModel> models = CCModel.parseObjModels(MekanismUtils.getResource(ResourceType.MODEL, "glow_panel.obj"), 7, null);

		frameModels = new CCModel[6];
		frameModels[0] = models.get("frame");
		CCModel.generateSidedModels(frameModels, 0, new Vector3(0, 0, 0));

		lightModels = new CCModel[6];
		lightModels[0] = models.get("light");
		CCModel.generateSidedModels(lightModels, 0, new Vector3(0, 0, 0));

		for(CCModel c : frameModels)
		{
			c.apply(new Translation(.5, .5, .5));
			c.computeLighting(LightModel.standardLightModel);
			c.shrinkUVs(0.0005);
		}

		for(CCModel c : lightModels)
		{
			c.apply(new Translation(.5, .5, .5));
			c.computeLighting(LightModel.standardLightModel);
			c.shrinkUVs(0.0005);
		}
	}

	public void renderStatic(PartGlowPanel panel, LazyLightMatrix olm)
	{
		TextureUtils.bindAtlas(0);
		CCRenderState.reset();
		CCRenderState.useModelColours(true);
		CCRenderState.setBrightness(panel.world(), panel.x(), panel.y(), panel.z());

		Colour colour = new ColourRGBA(panel.colour.getColor(0), panel.colour.getColor(1), panel.colour.getColor(2), 1);
		int side = panel.side.ordinal();

		frameModels[side].render(0, frameModels[side].verts.length, new Translation(panel.x(), panel.y(), panel.z()), new IconTransformation(icon), null);

		lightModels[side].render(0, lightModels[side].verts.length, new Translation(panel.x(), panel.y(), panel.z()), new IconTransformation(icon), new ColourMultiplier(colour));
	}

	public void renderItem(int metadata)
	{
		TextureUtils.bindAtlas(0);
		CCRenderState.reset();
		CCRenderState.startDrawing(7);
		CCRenderState.useModelColours(true);
		EnumColor c = EnumColor.DYES[metadata];

		Colour colour = new ColourRGBA(c.getColor(0), c.getColor(1), c.getColor(2), 1);
		Colour white = new ColourRGBA(1.0, 1.0, 1.0, 1.0);
		for(int i=4;i<5;i++){

		frameModels[i].render(0, frameModels[i].verts.length, new Translation(0, 0, 0), new IconTransformation(icon), new ColourMultiplier(white));
		lightModels[i].render(0, lightModels[i].verts.length, new Translation(0, 0, 0), new IconTransformation(icon), new ColourMultiplier(colour));
		}
		CCRenderState.draw();
	}

	@Override
	public void registerIcons(IconRegister register)
	{
		icon = register.registerIcon("mekanism:models/GlowPanel");
	}

	@Override
	public int atlasIndex()
	{
		return 0;
	}
}
