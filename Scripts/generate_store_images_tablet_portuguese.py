import os
from PIL import Image, ImageDraw, ImageFont

# --- Configuration ---
# Update these filenames to match your raw tablet screenshots
RAW_IMAGES = {
    "1_Inicio_Claro_Tablet": {
        "file": "inicio_claro_tablet.png",
        "title": "Confira Facilmente o Status dos Seus Amigos.",
        "is_dark": False
    },
    "2_Calendario_Tablet": {
        "file": "calendario_tablet.png",
        "title": "Gerencie Seus Planos sem Esforço.",
        "is_dark": False
    },
    "3_Circulos_Tablet": {
        "file": "circulos_tablet.png",
        "title": "Organize Círculos para Qualquer Grupo ou Ocasião.",
        "is_dark": False
    },
    "4_Configuracoes_Tablet": {
        "file": "configuracoes_tablet.png",
        "title": "Controle Total Sobre Quem Vê Sua Disponibilidade.",
        "is_dark": False
    },
    "5_Inicio_Escuro_Tablet": {
        "file": "inicio_escuro_tablet.png",
        "title": "Um Modo Escuro Incrível para Sua Comodidade.",
        "is_dark": True
    }
}

OUTPUT_DIR = "final_tablet_images"

# Standard Play Store Landscape Resolution
CANVAS_SIZE = (1920, 1080)

def draw_gradient(draw, width, height, is_dark):
    """Draws a premium background gradient to match the Free2Party branding."""
    if is_dark:
        for y in range(height):
            r = int(10 + (20 - 10) * (y / height))
            g = int(15 + (10 - 15) * (y / height))
            b = int(30 + (20 - 30) * (y / height))
            draw.line([(0, y), (width, y)], fill=(r, g, b, 255))
    else:
        for y in range(height):
            r = int(215 + (245 - 215) * (y / height))
            g = int(238 + (210 - 238) * (y / height))
            b = int(232 + (225 - 232) * (y / height))
            draw.line([(0, y), (width, y)], fill=(r, g, b, 255))

def create_framed_image(key, data):
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
        
    try:
        screenshot = Image.open(data["file"]).convert("RGBA")
    except FileNotFoundError:
        print(f"Skipping {key}: Could not find file '{data['file']}'")
        return

    # 1. Create Base Canvas & Apply Branding Gradient
    canvas = Image.new("RGBA", CANVAS_SIZE)
    draw = ImageDraw.Draw(canvas)
    draw_gradient(draw, CANVAS_SIZE[0], CANVAS_SIZE[1], data["is_dark"])
    
    # 2. Add High-Contrast Typography
    try:
        title_font = ImageFont.truetype("Montserrat-Bold.ttf", 72)
    except IOError:
        try:
            title_font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 72)
        except IOError:
            title_font = ImageFont.load_default()

    text_color = (255, 255, 255, 255) if data["is_dark"] else (20, 20, 20, 255)
    stroke_color = (0, 0, 0, 180) if data["is_dark"] else (255, 255, 255, 200)
    
    text_y = 100 
    
    words = data["title"].split()
    lines = []
    current_line = ""
    for word in words:
        if len(current_line + " " + word) < 45:
            current_line += " " + word if current_line else word
        else:
            lines.append(current_line)
            current_line = word
    if current_line:
        lines.append(current_line)
        
    for line in lines:
        draw.text(
            (CANVAS_SIZE[0] // 2, text_y), 
            line, 
            fill=text_color, 
            font=title_font, 
            anchor="mm",
            stroke_width=3, 
            stroke_fill=stroke_color
        )
        text_y += 90

    # 3. Handle Tablet Frame - ANCHORED TO BOTTOM
    original_w, original_h = screenshot.size
    
    padding_bottom = 60
    max_tablet_h = 700 # Leaves plenty of room for headlines at the top
    
    # Calculate scale based on height to ensure the whole screen fits
    ratio = max_tablet_h / float(original_h)
    tablet_h = max_tablet_h
    tablet_w = int(original_w * ratio)
    
    # Safety check: if the tablet screenshot is extremely wide, constrain by width instead
    max_tablet_w = 1700
    if tablet_w > max_tablet_w:
        tablet_w = max_tablet_w
        ratio = tablet_w / float(original_w)
        tablet_h = int(original_h * ratio)
    
    # Center horizontally
    tablet_x = (CANVAS_SIZE[0] - tablet_w) // 2
    
    # Anchor vertically to the bottom with padding
    tablet_y = CANVAS_SIZE[1] - tablet_h - padding_bottom
    
    corner_radius = 35 

    # Resize screenshot smoothly
    screenshot_scaled = screenshot.resize((tablet_w, tablet_h), Image.Resampling.LANCZOS)

    # Create clipping mask
    mask = Image.new("L", (tablet_w, tablet_h), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([0, 0, tablet_w, tablet_h], radius=corner_radius, fill=255)

    # Paste screenshot
    canvas.paste(screenshot_scaled, (tablet_x, tablet_y), mask=mask)

    # 4. Draw Outer Device Frame Rim
    frame_color = (40, 40, 42, 255) if data["is_dark"] else (230, 230, 235, 255)
    border_width = 16
    draw.rounded_rectangle(
        [tablet_x - border_width, tablet_y - border_width, tablet_x + tablet_w + border_width, tablet_y + tablet_h + border_width],
        radius=corner_radius + border_width,
        outline=frame_color,
        width=border_width
    )

    # 5. Save Finished Asset
    out_path = os.path.join(OUTPUT_DIR, f"{key}_final.png")
    canvas.save(out_path)
    print(f"Generated properly scaled landscape asset (Nav bar visible): {out_path}")

if __name__ == '__main__':
    print("Generating Landscape App Store Images...")
    for key, data in RAW_IMAGES.items():
        create_framed_image(key, data)
    print("\nComplete! Check the 'final_tablet_images' folder.")