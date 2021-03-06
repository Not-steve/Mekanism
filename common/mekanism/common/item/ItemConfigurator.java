package mekanism.common.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.common.IConfigurable;
import mekanism.common.IInvConfiguration;
import mekanism.common.PacketHandler;
import mekanism.common.PacketHandler.Transmission;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.tile.TileEntityBasicBlock;
import mekanism.common.tile.TileEntityElectricChest;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import buildcraft.api.tools.IToolWrench;

public class ItemConfigurator extends ItemEnergized implements IToolWrench
{
	public final int ENERGY_PER_CONFIGURE = 400;
	public final int ENERGY_PER_ITEM_DUMP = 8;

	private Random random = new Random();

	public ItemConfigurator(int id)
	{
		super(id, 60000);
	}

	@Override
	public void addInformation(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag)
	{
		super.addInformation(itemstack, entityplayer, list, flag);
		list.add(EnumColor.PINK + MekanismUtils.localize("gui.state") + ": " + EnumColor.GREY + getStateDisplay(getState(itemstack)));

		if(getState(itemstack) == 3)
		{
			if(hasLink(itemstack))
			{
				Coord4D obj = getLink(itemstack);

				list.add(EnumColor.GREY + MekanismUtils.localize("tooltip.configurator.linkMsg") + " " + EnumColor.INDIGO + MekanismUtils.getCoordDisplay(obj) + EnumColor.GREY + ", " + MekanismUtils.localize("tooltip.configurator.dim") + " " + EnumColor.INDIGO + obj.dimensionId);
			}
			else {
				list.add(EnumColor.GREY + MekanismUtils.localize("tooltip.configurator.noLink"));
			}
		}
	}

	@Override
	public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ)
	{
		if(!world.isRemote)
		{
			TileEntity tile = world.getBlockTileEntity(x, y, z);

			if(tile instanceof IConfigurable)
			{
				IConfigurable config = (IConfigurable)tile;

				if(player.isSneaking())
				{
					config.onSneakRightClick(player, side);
				}
				else {
					config.onRightClick(player, side);
				}
			}

			if(getState(stack) == 0)
			{
				if(tile instanceof IInvConfiguration)
				{
					IInvConfiguration config = (IInvConfiguration)tile;

					if(!player.isSneaking())
					{
						player.sendChatToPlayer(ChatMessageComponent.createFromText(EnumColor.DARK_BLUE + "[Mekanism]" + EnumColor.GREY + " " + MekanismUtils.localize("tooltip.configurator.viewColor") + ": " + config.getSideData().get(config.getConfiguration()[MekanismUtils.getBaseOrientation(side, config.getOrientation())]).color.getName()));
						return true;
					}
					else {
						if(getEnergy(stack) >= ENERGY_PER_CONFIGURE)
						{
							setEnergy(stack, getEnergy(stack) - ENERGY_PER_CONFIGURE);
							MekanismUtils.incrementOutput(config, MekanismUtils.getBaseOrientation(side, config.getOrientation()));
							player.sendChatToPlayer(ChatMessageComponent.createFromText(EnumColor.DARK_BLUE + "[Mekanism]" + EnumColor.GREY + " " + MekanismUtils.localize("tooltip.configurator.toggleColor") + ": " + config.getSideData().get(config.getConfiguration()[MekanismUtils.getBaseOrientation(side, config.getOrientation())]).color.getName()));

							if(config instanceof TileEntityBasicBlock)
							{
								TileEntityBasicBlock tileEntity = (TileEntityBasicBlock)config;
								PacketHandler.sendPacket(Transmission.CLIENTS_RANGE, new PacketTileEntity().setParams(Coord4D.get(tileEntity), tileEntity.getNetworkedData(new ArrayList())), Coord4D.get(tileEntity), 50D);
							}

							return true;
						}
					}
				}
			}
			else if(getState(stack) == 1)
			{
				if(tile instanceof IInventory)
				{
					int itemAmount = 0;
					IInventory inv = (IInventory)tile;

					if(!(inv instanceof TileEntityElectricChest) || (((TileEntityElectricChest)inv).canAccess()))
					{
						for(int i = 0; i < inv.getSizeInventory(); i++)
						{
							ItemStack slotStack = inv.getStackInSlot(i);

							if(slotStack != null)
							{
								if(getEnergy(stack) < ENERGY_PER_ITEM_DUMP)
								{
									break;
								}

								float xRandom = random.nextFloat() * 0.8F + 0.1F;
								float yRandom = random.nextFloat() * 0.8F + 0.1F;
								float zRandom = random.nextFloat() * 0.8F + 0.1F;

								while(slotStack.stackSize > 0)
								{
									int j = random.nextInt(21) + 10;

									if(j > slotStack.stackSize)
									{
										j = slotStack.stackSize;
									}

									slotStack.stackSize -= j;
									EntityItem item = new EntityItem(world, x + xRandom, y + yRandom, z + zRandom, new ItemStack(slotStack.itemID, j, slotStack.getItemDamage()));

									if(slotStack.hasTagCompound())
									{
										item.getEntityItem().setTagCompound((NBTTagCompound)slotStack.getTagCompound().copy());
									}

									float k = 0.05F;
									item.motionX = random.nextGaussian() * k;
									item.motionY = random.nextGaussian() * k + 0.2F;
									item.motionZ = random.nextGaussian() * k;
									world.spawnEntityInWorld(item);

									inv.setInventorySlotContents(i, null);
									setEnergy(stack, getEnergy(stack) - ENERGY_PER_ITEM_DUMP);
								}
							}
						}

						return true;
					}
					else {
						player.addChatMessage(EnumColor.DARK_BLUE + "[Mekanism] " + EnumColor.GREY + MekanismUtils.localize("tooltip.configurator.unauth"));
						return true;
					}
				}
			}
			else if(getState(stack) == 2)
			{
				if(tile instanceof TileEntityBasicBlock)
				{
					TileEntityBasicBlock basicBlock = (TileEntityBasicBlock)tile;
					int newSide = basicBlock.facing;

					if(!player.isSneaking())
					{
						newSide = side;
					}
					else {
						newSide = ForgeDirection.OPPOSITES[side];
					}

					if(basicBlock.canSetFacing(newSide))
					{
						basicBlock.setFacing((short)newSide);
						world.playSoundEffect(x, y, z, "random.click", 1.0F, 1.0F);
					}

					return true;
				}
			}
			else if(getState(stack) == 3)
			{
				if(!world.isRemote && player.isSneaking())
				{
					Coord4D obj = new Coord4D(x, y, z, world.provider.dimensionId);
					player.addChatMessage(EnumColor.DARK_BLUE + "[Mekanism]" + EnumColor.GREY + " Set link to block " + EnumColor.INDIGO + MekanismUtils.getCoordDisplay(obj) + EnumColor.GREY + ", dimension " + EnumColor.INDIGO + obj.dimensionId);
					setLink(stack, obj);

					return true;
				}
			}
		}

		return false;
	}

	public String getStateDisplay(int state)
	{
		switch(state)
		{
			case 0:
				return MekanismUtils.localize("tooltip.configurator.modify");
			case 1:
				return MekanismUtils.localize("tooltip.configurator.empty");
			case 2:
				return MekanismUtils.localize("tooltip.configurator.wrench");
			case 3:
				return MekanismUtils.localize("tooltip.configurator.link");
		}

		return "unknown";
	}

	public EnumColor getColor(int state)
	{
		switch(state)
		{
			case 0:
				return EnumColor.BRIGHT_GREEN;
			case 1:
				return EnumColor.AQUA;
			case 2:
				return EnumColor.YELLOW;
			case 3:
				return EnumColor.PINK;
		}

		return EnumColor.GREY;
	}

	public void setState(ItemStack itemstack, byte state)
	{
		if(itemstack.stackTagCompound == null)
		{
			itemstack.setTagCompound(new NBTTagCompound());
		}

		itemstack.stackTagCompound.setByte("state", state);
	}

	public byte getState(ItemStack itemstack)
	{
		if(itemstack.stackTagCompound == null)
		{
			return 0;
		}

		byte state = 0;

		if(itemstack.stackTagCompound.getTag("state") != null)
		{
			state = itemstack.stackTagCompound.getByte("state");
		}

		return state;
	}

	public boolean hasLink(ItemStack itemStack)
	{
		return getLink(itemStack) != null;
	}

	public Coord4D getLink(ItemStack itemStack)
	{
		if(itemStack.stackTagCompound == null || !itemStack.getTagCompound().hasKey("position"))
		{
			return null;
		}

		return Coord4D.read(itemStack.getTagCompound().getCompoundTag("position"));
	}

	public void setLink(ItemStack itemStack, Coord4D obj)
	{
		if(itemStack.getTagCompound() == null)
		{
			itemStack.setTagCompound(new NBTTagCompound());
		}

		itemStack.getTagCompound().setCompoundTag("position", obj.write(new NBTTagCompound()));
	}

	public void clearLink(ItemStack itemStack)
	{
		itemStack.getTagCompound().removeTag("position");
	}

	@Override
	public boolean canSend(ItemStack itemStack)
	{
		return false;
	}

	@Override
	public boolean canWrench(EntityPlayer player, int x, int y, int z)
	{
		return !(player.worldObj.getBlockTileEntity(x, y, z) instanceof TileEntityBasicBlock);
	}

	@Override
	public void wrenchUsed(EntityPlayer player, int x, int y, int z) {}
}
