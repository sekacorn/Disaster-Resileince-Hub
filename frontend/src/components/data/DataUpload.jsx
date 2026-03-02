import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { dataAPI } from '@services/api';
import { toast } from 'react-toastify';
import { FaUpload, FaFile, FaTimes, FaCheck } from 'react-icons/fa';

const DataUpload = () => {
  const [files, setFiles] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState({});

  const onDrop = useCallback((acceptedFiles) => {
    const newFiles = acceptedFiles.map((file) => ({
      file,
      id: Math.random().toString(36).substr(2, 9),
      status: 'pending',
    }));
    setFiles((prev) => [...prev, ...newFiles]);
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'text/csv': ['.csv'],
      'application/json': ['.json'],
      'application/vnd.ms-excel': ['.xls'],
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
      'application/geo+json': ['.geojson'],
    },
  });

  const removeFile = (id) => {
    setFiles((prev) => prev.filter((f) => f.id !== id));
  };

  const handleUpload = async () => {
    if (files.length === 0) {
      toast.error('Please select files to upload');
      return;
    }

    setUploading(true);

    for (const fileObj of files) {
      if (fileObj.status === 'success') continue;

      try {
        const formData = new FormData();
        formData.append('file', fileObj.file);
        formData.append('name', fileObj.file.name);
        formData.append('type', 'disaster_data');

        setFiles((prev) =>
          prev.map((f) =>
            f.id === fileObj.id ? { ...f, status: 'uploading' } : f
          )
        );

        await dataAPI.uploadData(formData);

        setFiles((prev) =>
          prev.map((f) =>
            f.id === fileObj.id ? { ...f, status: 'success' } : f
          )
        );

        toast.success(`${fileObj.file.name} uploaded successfully`);
      } catch (error) {
        setFiles((prev) =>
          prev.map((f) =>
            f.id === fileObj.id ? { ...f, status: 'error' } : f
          )
        );
        toast.error(`Failed to upload ${fileObj.file.name}`);
      }
    }

    setUploading(false);
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'success':
        return <FaCheck className="text-success-500" />;
      case 'error':
        return <FaTimes className="text-danger-500" />;
      case 'uploading':
        return <div className="spinner"></div>;
      default:
        return <FaFile className="text-gray-400" />;
    }
  };

  return (
    <div className="p-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold mb-2">Upload Disaster Data</h1>
        <p className="text-gray-600 dark:text-gray-400">
          Upload CSV, JSON, Excel, or GeoJSON files
        </p>
      </div>

      {/* Dropzone */}
      <div
        {...getRootProps()}
        className={`border-2 border-dashed rounded-lg p-12 text-center cursor-pointer transition-colors ${
          isDragActive
            ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
            : 'border-gray-300 dark:border-gray-700 hover:border-primary-400'
        }`}
      >
        <input {...getInputProps()} />
        <FaUpload className="w-12 h-12 mx-auto mb-4 text-gray-400" />
        {isDragActive ? (
          <p className="text-lg font-medium">Drop files here...</p>
        ) : (
          <>
            <p className="text-lg font-medium mb-2">
              Drag and drop files here, or click to select
            </p>
            <p className="text-sm text-gray-500">
              Supported formats: CSV, JSON, Excel, GeoJSON
            </p>
          </>
        )}
      </div>

      {/* File List */}
      {files.length > 0 && (
        <div className="card">
          <h2 className="text-xl font-bold mb-4">Selected Files</h2>
          <div className="space-y-2">
            {files.map((fileObj) => (
              <div
                key={fileObj.id}
                className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-900 rounded-lg"
              >
                <div className="flex items-center gap-3 flex-1">
                  {getStatusIcon(fileObj.status)}
                  <div className="flex-1">
                    <div className="font-medium">{fileObj.file.name}</div>
                    <div className="text-xs text-gray-500">
                      {(fileObj.file.size / 1024 / 1024).toFixed(2)} MB
                    </div>
                  </div>
                </div>
                {fileObj.status !== 'uploading' && (
                  <button
                    onClick={() => removeFile(fileObj.id)}
                    className="text-gray-400 hover:text-danger-500 transition-colors"
                  >
                    <FaTimes />
                  </button>
                )}
              </div>
            ))}
          </div>

          <div className="mt-4 flex gap-2">
            <button
              onClick={handleUpload}
              disabled={uploading}
              className="btn btn-primary flex-1"
            >
              {uploading ? 'Uploading...' : 'Upload All'}
            </button>
            <button
              onClick={() => setFiles([])}
              disabled={uploading}
              className="btn btn-secondary"
            >
              Clear All
            </button>
          </div>
        </div>
      )}

      {/* Upload Guidelines */}
      <div className="card bg-gray-50 dark:bg-gray-900">
        <h3 className="font-bold mb-3">Data Upload Guidelines</h3>
        <ul className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
          <li>• CSV files should include headers with column names</li>
          <li>• JSON files should follow the disaster data schema</li>
          <li>• GeoJSON files should contain valid geometry data</li>
          <li>• Maximum file size: 50 MB per file</li>
          <li>• Required fields: location, type, severity, timestamp</li>
          <li>• Optional fields: description, affected_population, casualties</li>
        </ul>
      </div>
    </div>
  );
};

export default DataUpload;
