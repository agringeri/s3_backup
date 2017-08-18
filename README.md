# S3 Backup

This program is intended for the use of backing up regularly-generated files to Amazon S3.

### Using S3 Backup

To upload a file to Amazon S3 using this program, run from the command line and pass in an absolute path to the file
you would like to upload. S3 Backup handles the backup by placing the backup in the appropriate places, ensuring
backups are regularly kept. Changing values within the config.json file will control how many backups are kept within
Amazon S3.

### Caveats

* This program only handles Amazon S3 objects that are in the STANDARD storage class. Any objects that are not in the
STANDARD storage class will be ignored.

* Use Amazon S3 bucket lifecycle rules to control GLACIER storage. All GLACIER objects that are stored in S3 will be
ignored and separately handled within the Amazon S3 console itself.

