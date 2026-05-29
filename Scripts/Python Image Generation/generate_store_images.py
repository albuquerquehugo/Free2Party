import os
from PIL import Image, ImageDraw, ImageFont

# --- Configuration ---
RAW_IMAGES = {
    "1_Home_Light": {
        "file": "home_light.png",
        "title": "Easily Check Your Friends' Status.",
        "is_dark": False
    },
    "2_Calendar": {
        "file": "calendar.png",
        "title": "Manage Your Plans Effortlessly.",
        "is_dark": False
    },
    "3_Circles": {
        "file": "circles.png",
        "title": "Organize Circles for Any Group or Occasion.",
        "is_dark": False
    },
    "4_Settings": {
        "file": "settings.png",
        "title": "Total Control Over Who Sees Your Availability.",
        "is_dark": False
    },
    "5_Home_Dark": {
        "file": "home_dark.png",
        "title": "A Stunning Dark Mode For Your Convenience.",
        "is_dark": True
    }
}

OUTPUT_DIR = "final_store_images"
CANVAS_SIZE = (1080, 1920)

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
    
    # 2. Add High-Contrast Typography with Montserrat
    try:
        # Looks for the Montserrat font file in the same directory as the script
        title_font = ImageFont.truetype("Montserrat-Bold.ttf", 72)
    except IOError:
        print("Warning: Could not find 'Montserrat-Bold.ttf' in the folder. Falling back to default.")
        try:
            title_font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 72)
        except IOError:
            title_font = ImageFont.load_default()

    text_color = (255, 255, 255, 255) if data["is_dark"] else (20, 20, 20, 255)
    stroke_color = (0, 0, 0, 180) if data["is_dark"] else (255, 255, 255, 200)
    
    # Adjusted slightly higher for Montserrat's baseline
    text_y = 100 
    
    words = data["title"].split()
    lines = []
    current_line = ""
    for word in words:
        if len(current_line + " " + word) < 22:
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

    # 3. Handle Smartphone Frame - ANCHORED TO BOTTOM
    original_w, original_h = screenshot.size
    
    padding_bottom = 80
    max_phone_h = 1450 
    
    ratio = max_phone_h / float(original_h)
    phone_h = max_phone_h
    phone_w = int(original_w * ratio)
    
    phone_x = (CANVAS_SIZE[0] - phone_w) // 2
    phone_y = CANVAS_SIZE[1] - phone_h - padding_bottom
    
    corner_radius = 55 

    screenshot_scaled = screenshot.resize((phone_w, phone_h), Image.Resampling.LANCZOS)

    mask = Image.new("L", (phone_w, phone_h), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([0, 0, phone_w, phone_h], radius=corner_radius, fill=255)

    canvas.paste(screenshot_scaled, (phone_x, phone_y), mask=mask)

    # 4. Draw Outer Device Frame Rim
    frame_color = (230, 230, 235, 255) if data["is_dark"] else (40, 40, 42, 255)
    border_width = 16
    draw.rounded_rectangle(
        [phone_x - border_width, phone_y - border_width, phone_x + phone_w + border_width, phone_y + phone_h + border_width],
        radius=corner_radius + border_width,
        outline=frame_color,
        width=border_width
    )

    # 5. Save Finished Asset
    out_path = os.path.join(OUTPUT_DIR, f"{key}_final.png")
    canvas.save(out_path)
    print(f"Generated properly scaled asset with Montserrat: {out_path}")

if __name__ == '__main__':
    print("Generating App Store Images...")
    for key, data in RAW_IMAGES.items():
        create_framed_image(key, data)
    print("\nComplete! Check the 'final_store_images' folder.")