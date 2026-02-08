import os
from PIL import Image, ImageDraw

JEWELS = [
    "artillery_jewel", "bandolier_jewel", "blastcoat_jewel", "blunt_jewel", "charge_jewel",
    "charge_up_jewel", "crit_element_jewel", "crit_status_jewel", "critical_jewel",
    "drain_jewel", "draincoat_jewel", "draw_jewel", "enhancer_jewel", "expert_jewel",
    "flight_jewel", "forceshot_jewel", "gambit_jewel", "grinder_jewel", "guardian_jewel",
    "handicraft_jewel", "ironwall_jewel", "ko_jewel", "magazine_jewel", "mastery_jewel",
    "minds_eye_jewel", "opener_jewel", "paracoat_jewel", "pierce_jewel", "poisoncoat_jewel",
    "precise_jewel", "quickswitch_jewel", "razor_sharp_jewel", "salvo_jewel", "sharp_jewel",
    "shield_jewel", "sleepcoat_jewel", "sonorous_jewel", "spread_jewel", "trueshot_jewel",
    "venom_jewel", "adapt_jewel", "ambush_jewel", "antiblast_jewel", "antidote_jewel",
    "antipara_jewel", "bomber_jewel", "botany_jewel", "brace_jewel", "chain_jewel",
    "challenger_jewel", "climber_jewel", "counter_jewel", "counterattack_jewel",
    "def_lock_jewel", "destroyer_jewel", "dive_jewel", "dragon_res_jewel", "earplugs_jewel",
    "enduring_jewel", "escape_jewel", "fire_res_jewel", "flash_jewel", "flawless_jewel",
    "flayer_jewel", "footing_jewel", "foray_jewel", "friendship_jewel", "fungiform_jewel",
    "furor_jewel", "geology_jewel", "gobbler_jewel", "growth_jewel", "hungerless_jewel",
    "ice_res_jewel", "intimidator_jewel", "jumping_jewel", "leap_jewel", "maintenance_jewel",
    "medicine_jewel", "mighty_jewel", "mirewalker_jewel", "pep_jewel", "perfume_jewel",
    "phoenix_jewel", "physique_jewel", "potential_jewel", "protection_jewel", "ranger_jewel",
    "recovery_jewel", "refresh_jewel", "sane_jewel", "satiated_jewel", "sheath_jewel",
    "shockproof_jewel", "specimen_jewel", "sprinter_jewel", "steadfast_jewel", "survival_jewel",
    "suture_jewel", "tenderizer_jewel", "throttle_jewel", "thunder_res_jewel", "water_res_jewel",
    "wind_resist_jewel",
    # Added ones
    "attack_jewel", "blaze_jewel", "stream_jewel", "bolt_jewel", "frost_jewel", "dragon_jewel",
    "blast_jewel", "paralyzer_jewel", "sleep_jewel", "defense_jewel", "vitality_jewel", "evasion_jewel"
]

OUTPUT_DIR = "src/main/resources/assets/mhweaponsmod/textures/item/"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# Color Map (R, G, B)
COLORS = {
    "red": (200, 50, 50),
    "orange": (220, 120, 40),
    "yellow": (220, 200, 50),
    "green": (50, 200, 50),
    "cyan": (50, 200, 200),
    "blue": (50, 50, 200),
    "purple": (150, 50, 200),
    "white": (220, 220, 220),
    "grey": (150, 150, 150),
    "black": (50, 50, 50),
    "pink": (220, 100, 180),
    "brown": (139, 69, 19)
}

def get_color(name):
    n = name.lower()
    if "attack" in n or "challenger" in n or "furor" in n or "resentment" in n: return COLORS["red"]
    if "fire" in n or "blaze" in n or "bomber" in n or "blast" in n or "artillery" in n: return COLORS["orange"]
    if "water" in n or "stream" in n: return COLORS["blue"]
    if "thunder" in n or "bolt" in n or "paralyzer" in n or "antipara" in n: return COLORS["yellow"]
    if "ice" in n or "frost" in n: return COLORS["cyan"]
    if "dragon" in n: return COLORS["purple"]
    if "poison" in n or "venom" in n: return COLORS["purple"]
    if "sleep" in n: return COLORS["grey"]
    if "defense" in n or "ironwall" in n or "shield" in n or "protection" in n or "guardian" in n: return COLORS["brown"]
    if "expert" in n or "critical" in n or "affinity" in n or "chain" in n: return COLORS["cyan"]
    if "tenderizer" in n: return COLORS["orange"]
    if "vitality" in n or "medicine" in n or "recovery" in n: return COLORS["green"]
    if "evasion" in n or "jumping" in n or "leap" in n or "footing" in n: return COLORS["green"]
    if "stamina" in n or "sprinter" in n or "physique" in n or "refresh" in n: return COLORS["yellow"]
    if "earplugs" in n or "wind" in n: return COLORS["grey"]
    if "handicraft" in n or "sharp" in n or "razor" in n: return COLORS["white"]
    if "pierce" in n: return COLORS["blue"]
    if "spread" in n: return COLORS["green"]
    if "forceshot" in n: return COLORS["white"]
    if "ko" in n or "slugger" in n: return COLORS["grey"]
    if "drain" in n: return COLORS["black"]
    return COLORS["white"] # Default

def generate_icon(name):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    base_color = get_color(name)
    outline_color = (30, 30, 30)
    
    # Draw gem shape (diamond/hexagon ish)
    # Simple Square with border for now, maybe rotated
    # Let's do a diamond shape
    
    # Diamond coordinates: (8,2), (14,8), (8,14), (2,8)
    draw.polygon([(8, 1), (15, 8), (8, 15), (1, 8)], fill=base_color, outline=outline_color)
    
    # Add a shine/highlight
    highlight_color = (min(255, base_color[0]+50), min(255, base_color[1]+50), min(255, base_color[2]+50))
    draw.line([(8, 3), (12, 7)], fill=highlight_color, width=1)
    # draw.point((8,3), fill=highlight_color)
    
    filename = os.path.join(OUTPUT_DIR, f"{name}.png")
    img.save(filename)
    # print(f"Generated {filename}")

if __name__ == "__main__":
    count = 0
    for jewel in JEWELS:
        generate_icon(jewel)
        count += 1
    print(f"Generated {count} jewel icons.")
