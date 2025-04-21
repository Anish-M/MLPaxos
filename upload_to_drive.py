import glob
import re
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

def return_server_messages():
    """
    Finds all server_*_messages.txt files and maps extracted numbers to filenames.
    """
    file_pattern = "server_*_messages.txt"
    file_list = {}

    # Loop through matching files
    for file_name in glob.glob(file_pattern):
        match = re.search(r"server_(\d+)_messages\.txt", file_name)
        if match:
            number = int(match.group(1))  # Extract the number
            file_list[number] = file_name  # Store in dictionary
    return file_list

def empty_folder_from_drive(folder_id):
    """
    Empty a Google Drive folder by deleting all files within it.
    
    :param folder_id: Google Drive folder ID to empty.
    """
    creds = service_account.Credentials.from_service_account_file('./GDriveCreds/macro-centaur-421022-5219f036c90f.json')
    drive_service = build('drive', 'v3', credentials=creds)

    # List files in the folder
    query = f"'{folder_id}' in parents"
    results = drive_service.files().list(q=query, fields="files(id)").execute()
    items = results.get('files', [])

    # Delete each file
    for item in items:
        drive_service.files().delete(fileId=item['id']).execute()
        print(f"Deleted file with ID: {item['id']}")

    print(f"Emptied folder with ID: {folder_id}")


def upload_file_to_drive(file_path, folder_id=None):
    """
    Upload a file to Google Drive.
    
    :param file_path: Local path of the file to upload.
    :param folder_id: Google Drive folder ID where the file should be uploaded (optional).
    """
    creds = service_account.Credentials.from_service_account_file('./GDriveCreds/macro-centaur-421022-5219f036c90f.json')
    drive_service = build('drive', 'v3', credentials=creds)

    # File metadata
    file_metadata = {"name": file_path.split("/")[-1]}
    
    if folder_id:
        file_metadata["parents"] = [folder_id]

    # Upload the file
    media = MediaFileUpload(file_path, resumable=True)
    file = drive_service.files().create(body=file_metadata, media_body=media, fields="id").execute()
    
    print(f"Uploaded {file_path} to Google Drive. File ID: {file['id']}")


# Get files to upload
file_list = return_server_messages()

# Mapping of extracted numbers to Google Drive folder IDs
folder_ids = {
    0: "1v6qllq91a1xHFevxuA2onT_mCib_4u4f",
    1: "1B2dGDDe3az4RSVb6H9-ISrlRZ1DG020N",
    2: "1mQ9Fi23UBiHY1tICtyq6CSAmOd4cFWKH",
}

# create an loop for 2 hours that replaces the existing file in the folder and uploads the new local one
import time

start_time = time.time()
duration = 2 * 60 * 60  # 2 hours in seconds

while time.time() - start_time < duration:
    for number, file_path in file_list.items():
        folder_id = folder_ids.get(number)
        if folder_id:
            empty_folder_from_drive(folder_id)
            empty_folder_from_drive(folder_id='1MhejMNR1Y7JtiJ92J3-Cy9eysEWM3ydE')
            upload_file_to_drive(file_path, folder_id)
            upload_file_to_drive('./tracking_failures.txt', folder_id='1MhejMNR1Y7JtiJ92J3-Cy9eysEWM3ydE')
            time.sleep(10)  # Sleep for 5 seconds between uploads
