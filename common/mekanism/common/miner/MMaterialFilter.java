package mekanism.common.miner;

import java.util.ArrayList;

import mekanism.common.transporter.Finder.MaterialFinder;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.google.common.io.ByteArrayDataInput;

public class MMaterialFilter extends MinerFilter
{
	public ItemStack materialItem;
	
	public Material getMaterial()
	{
		return Block.blocksList[materialItem.itemID].blockMaterial;
	}

	@Override
	public boolean canFilter(ItemStack itemStack)
	{
		if(itemStack == null || !(itemStack.getItem() instanceof ItemBlock))
		{
			return false;
		}

		return new MaterialFinder(getMaterial()).modifies(itemStack);
	}

	@Override
	public NBTTagCompound write(NBTTagCompound nbtTags)
	{
		nbtTags.setInteger("type", 2);
		materialItem.writeToNBT(nbtTags);

		return nbtTags;
	}

	@Override
	protected void read(NBTTagCompound nbtTags)
	{
		materialItem = ItemStack.loadItemStackFromNBT(nbtTags);
	}

	@Override
	public void write(ArrayList data)
	{
		data.add(2);

		data.add(materialItem.itemID);
		data.add(materialItem.stackSize);
		data.add(materialItem.getItemDamage());
	}

	@Override
	protected void read(ByteArrayDataInput dataStream)
	{
		materialItem = new ItemStack(dataStream.readInt(), dataStream.readInt(), dataStream.readInt());
	}

	@Override
	public int hashCode()
	{
		int code = 1;
		code = 31 * code + materialItem.itemID;
		code = 31 * code + materialItem.stackSize;
		code = 31 * code + materialItem.getItemDamage();
		return code;
	}

	@Override
	public boolean equals(Object filter)
	{
		return super.equals(filter) && filter instanceof MMaterialFilter && ((MMaterialFilter)filter).materialItem.isItemEqual(materialItem);
	}

	@Override
	public MMaterialFilter clone()
	{
		MMaterialFilter filter = new MMaterialFilter();
		filter.materialItem = materialItem;

		return filter;
	}
}
