package com.ohmygotto;

public class Weapon {
    public String id;
    public String name;
    public String displayName;
    public int ammo;
    public int maxAmmo;
    public double fireRate;
    public double damage;
    public double range; // lifespan in seconds

    public Weapon(String id, int ammo, int maxAmmo, double fireRate, double damage, double range) {
        this.id = id;
        this.ammo = ammo;
        this.maxAmmo = maxAmmo;
        this.fireRate = fireRate;
        this.damage = damage;
        this.range = range;
        this.displayName = getFullName(id);
    }

    private String getFullName(String id) {
        switch(id) {
            case "SG": return "Shotgun";
            case "SMG": return "Submachine Gun";
            case "AR": return "Assault Rifle";
            case "GL": return "Grenade Launcher";
            case "HG": return "Handgun";
            case "SR": return "Sniper Rifle";
            case "RG": return "Railgun";
            case "MG": return "Minigun";
            case "RL": return "Rocket Launcher";
            case "MT": return "Mortar";
            case "FT": return "Flamethrower";
            default: return "Unknown Weapon";
        }
    }

    public String getDisplayName() {
        return displayName + " (" + id + ")";
    }
    
    public boolean canFire() {
        return ammo > 0;
    }

    public void reload() {
        ammo = maxAmmo;
    }

    public String getName() {
        return name;
    }
    
    public String getID() {
        return id;
    }
}