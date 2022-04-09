package org.cloudwarp.probablychests.entity;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.cloudwarp.probablychests.registry.PCProperties;
import org.cloudwarp.probablychests.utils.PCChestState;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.EnumSet;

public class PCChestMimic extends PathAwareEntity implements IAnimatable {
	private AnimationFactory factory = new AnimationFactory(this);
	public static final AnimationBuilder IDLE = new AnimationBuilder().addAnimation("idle", true);
	public static final AnimationBuilder JUMP = new AnimationBuilder().addAnimation("jump", false);
	public static final AnimationBuilder CLOSE = new AnimationBuilder().addAnimation("close", false);
	//public static final AnimationBuilder CLOSED = new AnimationBuilder().addAnimation("closed", true);
	//public static final AnimationBuilder IN_AIR = new AnimationBuilder().addAnimation("inAir", true);
	private static final String CONTROLLER_NAME = "mimicController";
	private boolean onGroundLastTick;
	private static final TrackedData<Boolean> IS_JUMPING = DataTracker.registerData(PCChestMimic.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> IS_IDLE = DataTracker.registerData(PCChestMimic.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> IS_CLOSED = DataTracker.registerData(PCChestMimic.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> IS_GROUNDED = DataTracker.registerData(PCChestMimic.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> IS_FLYING = DataTracker.registerData(PCChestMimic.class, TrackedDataHandlerRegistry.BOOLEAN);
	public boolean isJumping = false;


	public PCChestMimic(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.ignoreCameraFrustum = true;
		this.moveControl = new PCChestMimic.MimicMoveControl(this);
	}

	protected void initGoals() {
		this.goalSelector.add(2, new PCChestMimic.FaceTowardTargetGoal(this));
		this.goalSelector.add(7, new PCChestMimic.IdleGoal(this));
		this.goalSelector.add(5, new PCChestMimic.MoveGoal(this));
		this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, livingEntity -> Math.abs(livingEntity.getY() - this.getY()) <= 4.0D));
	}

	public static DefaultAttributeContainer.Builder createMobAttributes() {
		return LivingEntity.createLivingAttributes().add(EntityAttributes.GENERIC_FOLLOW_RANGE, 26.0D)
				.add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED,1)
				.add(EntityAttributes.GENERIC_MAX_HEALTH,20)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,0.5D);
	}
	private <E extends IAnimatable> PlayState devMovement(AnimationEvent<E> animationEvent) {
		final AnimationController animationController = animationEvent.getController();
		AnimationBuilder builder = new AnimationBuilder();
		System.out.println(this.isJumping() + "  " + this.isGrounded() + "  " + this.isFlying() + "  " + this.isIdle() + " " + this.isJumping);
		if(this.isJumping()){
			if(!this.isJumping){
				builder.addAnimation("jump",false);
				this.isJumping = true;
				this.setIsJumping(false);
			}
			System.out.println("A");
		}else if(this.isJumping) {
			System.out.println("B");
			if (!animationEvent.getController().isJustStarting && animationEvent.getController().getAnimationState() == AnimationState.Stopped) {
				System.out.println("C");
				this.isJumping = false;
				this.setIsFlying(true);
			}
		}else if(this.isFlying() && this.isGrounded() && !this.isJumping() && !this.isIdle()){
			System.out.println("D");
			this.setIsFlying(false);
			builder.addAnimation("close",false);
		}else if(this.isFlying() && !this.isGrounded() && !this.isJumping() && !this.isIdle()){
			System.out.println("E");

		}else if(!this.isFlying() && this.isGrounded() && !this.isJumping() && this.isIdle()){
			System.out.println("F");
			builder.addAnimation("idle",false);
		}

		animationController.setAnimation(builder);
		return PlayState.CONTINUE;
	}


	@Override
	public void registerControllers (AnimationData animationData) {
		animationData.addAnimationController(new AnimationController(this, CONTROLLER_NAME, 0, this::devMovement));
	}

	@Override
	public AnimationFactory getFactory () {
		return this.factory;
	}
	private static class MimicMoveControl extends MoveControl {
		private float targetYaw;
		private int ticksUntilJump;
		private final PCChestMimic mimic;
		private boolean jumpOften;

		public MimicMoveControl(PCChestMimic mimic) {
			super(mimic);
			this.mimic = mimic;
			this.targetYaw = 180.0F * mimic.getYaw() / 3.1415927F;
		}

		public void look(float targetYaw, boolean jumpOften) {
			this.targetYaw = targetYaw;
			this.jumpOften = jumpOften;
		}

		public void move(double speed) {
			this.speed = speed;
			this.state = State.MOVE_TO;
		}

		public void tick() {
			this.entity.setYaw(this.wrapDegrees(this.entity.getYaw(), this.targetYaw, 90.0F));
			this.entity.headYaw = this.entity.getYaw();
			this.entity.bodyYaw = this.entity.getYaw();
			if (this.state != State.MOVE_TO) {
				this.entity.setForwardSpeed(0.0F);
			} else {
				this.state = State.WAIT;
				if (this.entity.isOnGround()) {
					this.entity.setMovementSpeed((float)(this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)));
					if (this.ticksUntilJump-- <= 0) {
						this.ticksUntilJump = this.mimic.getTicksUntilNextJump();
						if (this.jumpOften) {
							this.ticksUntilJump /= 3;
						}

						this.mimic.getJumpControl().setActive();
						this.mimic.playSound(this.mimic.getJumpSound(), this.mimic.getSoundVolume(), this.mimic.getJumpSoundPitch());
					} else {
						this.mimic.sidewaysSpeed = 0.0F;
						this.mimic.forwardSpeed = 0.0F;
						this.entity.setMovementSpeed(0.0F);
					}
				} else {
					this.entity.setMovementSpeed((float)(this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)));
				}

			}
		}
	}

	static class FaceTowardTargetGoal extends Goal {
		private final PCChestMimic mimic;
		private int ticksLeft;

		public FaceTowardTargetGoal(PCChestMimic mimic) {
			this.mimic = mimic;
			this.setControls(EnumSet.of(Control.LOOK));
		}

		public boolean canStart() {
			LivingEntity livingEntity = this.mimic.getTarget();
			if (livingEntity == null) {
				return false;
			} else {
				return !this.mimic.canTarget(livingEntity) ? false : this.mimic.getMoveControl() instanceof PCChestMimic.MimicMoveControl;
			}
		}

		public void start() {
			this.ticksLeft = toGoalTicks(300);
			super.start();
		}

		public boolean shouldContinue() {
			LivingEntity livingEntity = this.mimic.getTarget();
			if (livingEntity == null) {
				return false;
			} else if (!this.mimic.canTarget(livingEntity)) {
				return false;
			} else {
				return --this.ticksLeft > 0;
			}
		}

		public boolean shouldRunEveryTick() {
			return true;
		}

		public void tick() {
			LivingEntity livingEntity = this.mimic.getTarget();
			if (livingEntity != null) {
				this.mimic.lookAtEntity(livingEntity, 10.0F, 10.0F);
			}

			((PCChestMimic.MimicMoveControl)this.mimic.getMoveControl()).look(this.mimic.getYaw(), this.mimic.canAttack());
		}
	}
	static class MoveGoal extends Goal {
		private final PCChestMimic mimic;

		public MoveGoal(PCChestMimic mimic) {
			this.mimic = mimic;
			this.setControls(EnumSet.of(Control.JUMP, Control.MOVE));
		}

		public boolean canStart() {
			//return !this.mimc.hasVehicle();
			return this.mimic.getTarget() != null;
		}

		public void tick() {
			((PCChestMimic.MimicMoveControl)this.mimic.getMoveControl()).move(2.5D);
		}
	}

	static class IdleGoal extends Goal {
		private final PCChestMimic mimic;

		public IdleGoal(PCChestMimic mimic) {
			this.mimic = mimic;

		}

		public boolean canStart() {
			return !this.mimic.hasVehicle();
		}

		public void tick() {

		}
	}

	float getJumpSoundPitch() {
		return ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
	}

	protected SoundEvent getJumpSound() {
		return SoundEvents.BLOCK_CHEST_OPEN;
	}

	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR;
	}

	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR;
	}

	protected SoundEvent getLandingSound() {
		return SoundEvents.BLOCK_CHEST_CLOSE;
	}

	protected void jump() {
		Vec3d vec3d = this.getVelocity();
		this.setVelocity(vec3d.x, (double)this.getJumpVelocity(), vec3d.z);
		this.velocityDirty = true;
		System.out.println("HERE1");
		this.setIsJumping(true);
	}

	protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
		return 0.625F * dimensions.height;
	}

	protected boolean canAttack() {
		return true;
	}

	protected float getDamageAmount() {
		return (float)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
	}

	public void onPlayerCollision(PlayerEntity player) {
		if (this.canAttack()) {
			this.damage(player);
		}

	}

	protected void damage(LivingEntity target) {
		if (this.isAlive()) {
			if (this.squaredDistanceTo(target) < 1.2D && this.canSee(target) && target.damage(DamageSource.mob(this), this.getDamageAmount())) {
				this.playSound(SoundEvents.BLOCK_CHEST_CLOSE, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
				this.applyDamageEffects(this, target);
			}
		}
	}

	protected int getTicksUntilNextJump() {
		return this.random.nextInt(5) + 5;
	}

	public void tick() {
		super.tick();
		if (this.onGround && !this.onGroundLastTick) {
			// play landing
			this.playSound(this.getLandingSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
		} else if (!this.onGround && this.onGroundLastTick) {
			// set flying animation to true
		}

		this.onGroundLastTick = this.onGround;
	}

	protected boolean isDisallowedInPeaceful() {
		return true;
	}

	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("wasOnGround", this.onGroundLastTick);
		/*nbt.putBoolean("grounded",this.isOnGround());
		nbt.putBoolean("jumping",this.isJumping());
		nbt.putBoolean("idle",this.isIdle());
		nbt.putBoolean("flying",this.isFlying());*/
	}

	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.onGroundLastTick = nbt.getBoolean("wasOnGround");
		/*this.setIsGrounded(nbt.getBoolean("grounded"));
		this.setIsJumping(nbt.getBoolean("jumping"));
		this.setIsIdle(nbt.getBoolean("idle"));
		this.setIsFlying(nbt.getBoolean("flying"));*/
	}

	public void setIsJumping(boolean jumping) {
		this.dataTracker.set(IS_JUMPING, jumping);
	}

	public boolean isJumping() {
		return this.dataTracker.get(IS_JUMPING);
	}

	public void setIsFlying(boolean flying) {
		this.dataTracker.set(IS_FLYING, flying);
	}

	public boolean isFlying() {
		return this.dataTracker.get(IS_FLYING);
	}

	public void setIsGrounded(boolean grounded) {
		this.dataTracker.set(IS_GROUNDED, grounded);
	}

	public boolean isGrounded() {
		return this.isOnGround();
		//return this.dataTracker.get(IS_GROUNDED);
	}

	public void setIsIdle(boolean idle) {
		this.dataTracker.set(IS_IDLE, idle);
	}

	public boolean isIdle() {
		return this.dataTracker.get(IS_IDLE);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		this.dataTracker.startTracking(IS_GROUNDED, true);
		this.dataTracker.startTracking(IS_JUMPING, false);
		this.dataTracker.startTracking(IS_FLYING, false);
		this.dataTracker.startTracking(IS_IDLE, false);

	}

}