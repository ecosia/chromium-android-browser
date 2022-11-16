import argparse
import zipfile
import os
import shutil

def extract_zip(zip_filename, extract_folder):
    with zipfile.ZipFile(zip_filename, 'r') as zip_ref:
        zip_ref.extractall(extract_folder)

def move_images_to_root(folder):
    for root, dirs, files in os.walk(folder):
        for file in files:
            if file.endswith(('.jpg', '.jpeg', '.png', '.gif')):
                src_path = os.path.join(root, file)
                dest_path = os.path.join(folder, file)
                shutil.move(src_path, dest_path)

def cleanup_empty_folders(folder):
    for root, dirs, files in os.walk(folder, topdown=False):
        for dir in dirs:
            dir_path = os.path.join(root, dir)
            if not os.listdir(dir_path):
                os.rmdir(dir_path)

def create_drawable_folders(base_folder, is_night):
    image_resolutions = ['mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi']
    folder_prefix = 'night-' if is_night else ''

    for resolution in image_resolutions:
        folder_name = f'drawable-{folder_prefix}{resolution}'
        os.makedirs(os.path.join(base_folder, folder_name), exist_ok=True)

def copy_images_to_drawable_folders(base_folder, is_night):
    density_suffix = "-night" if is_night else ""
    density_folders = ['mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi']

    image_extensions = ['.png', '.jpg', '.jpeg', '.gif']  # Add more extensions if needed

    for density_folder in density_folders:
        for file in os.listdir(base_folder):
            if any(file.endswith(ext) for ext in image_extensions) and file.endswith(f'_{density_folder}.png'):
                src_path = os.path.join(base_folder, file)
                dest_folder = f'drawable{density_suffix}-{density_folder}'
                dest_path = os.path.join(base_folder, dest_folder, file)

                if os.path.exists(dest_path):
                    print(f"Skipping {file} for {dest_folder} - File already exists.")
                else:
                    print(f"Copying {file} to {dest_folder}")
                    os.makedirs(os.path.dirname(dest_path), exist_ok=True)
                    shutil.copy(src_path, dest_path)

def delete_images_on_root(folder):
    for file in os.listdir(folder):
        if file.endswith(('.jpg', '.jpeg', '.png', '.gif')):
            os.remove(os.path.join(folder, file))

def rename_images_in_subfolders(base_folder, new_name):
    for root, dirs, files in os.walk(base_folder):
        for dir in dirs:
            dir_path = os.path.join(root, dir)
            for file in os.listdir(dir_path):
                if file.endswith(('.jpg', '.jpeg', '.png', '.gif')):
                    old_path = os.path.join(dir_path, file)
                    extension = os.path.splitext(file)[1]
                    new_path = os.path.join(dir_path, f'{new_name}{extension}')
                    os.rename(old_path, new_path)

def copy_drawable_folders(source_dir, target_dir):
    density_folders = ['mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi']

    for density_folder in density_folders:
        source_night_density_folder = os.path.join(source_dir, f'drawable-night-{density_folder}')
        target_night_density_folder = os.path.join(target_dir, f'drawable-night-{density_folder}')

        source_day_density_folder = os.path.join(source_dir, f'drawable-{density_folder}')
        target_day_density_folder = os.path.join(target_dir, f'drawable-{density_folder}')

        if os.path.exists(source_night_density_folder):
            if not os.path.exists(target_night_density_folder):
                os.makedirs(target_night_density_folder)
            print(f"Copying contents from {source_night_density_folder} to {target_night_density_folder}")
            for item in os.listdir(source_night_density_folder):
                source_item = os.path.join(source_night_density_folder, item)
                target_item = os.path.join(target_night_density_folder, item)
                if os.path.isdir(source_item):
                    shutil.copytree(source_item, target_item, dirs_exist_ok=True)
                else:
                    shutil.copy2(source_item, target_item)

        if os.path.exists(source_day_density_folder):
            if not os.path.exists(target_day_density_folder):
                os.makedirs(target_day_density_folder)
            print(f"Copying contents from {source_day_density_folder} to {target_day_density_folder}")
            for item in os.listdir(source_day_density_folder):
                source_item = os.path.join(source_day_density_folder, item)
                target_item = os.path.join(target_day_density_folder, item)
                if os.path.isdir(source_item):
                    shutil.copytree(source_item, target_item, dirs_exist_ok=True)
                else:
                    shutil.copy2(source_item, target_item)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Extract, organize, and rename Figma images from a ZIP file.")
    parser.add_argument("zip_filename", help="Path to the ZIP file")
    parser.add_argument("output_path", help="Path to the output folder")
    parser.add_argument("new_image_name", help="New name for the images")
    parser.add_argument("--night", action="store_true", help="Use night drawable folders")
    args = parser.parse_args()

    figma_temp_folder = os.path.join(args.output_path, 'figma_temp')

    if os.path.exists(figma_temp_folder):
        shutil.rmtree(figma_temp_folder)

    os.makedirs(figma_temp_folder)
    extract_zip(args.zip_filename, figma_temp_folder)

    move_images_to_root(figma_temp_folder)
    cleanup_empty_folders(figma_temp_folder)

    create_drawable_folders(figma_temp_folder, args.night)

    copy_images_to_drawable_folders(figma_temp_folder, args.night)

    delete_images_on_root(figma_temp_folder)

    rename_images_in_subfolders(figma_temp_folder, args.new_image_name)

    copy_drawable_folders(figma_temp_folder, args.output_path)

    shutil.rmtree(figma_temp_folder)

    print("Zip file contents extracted and organized, images moved to drawable folders and renamed successfully!")
