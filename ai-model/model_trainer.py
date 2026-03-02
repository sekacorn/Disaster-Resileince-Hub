"""
PyTorch Model Training Script for Disaster Risk Prediction
Trains the neural network on historical disaster data
"""

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader, random_split
import numpy as np
import pandas as pd
from typing import Tuple, Optional
import logging
from pathlib import Path
import json
from datetime import datetime

from predictor import DisasterRiskModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class DisasterDataset(Dataset):
    """PyTorch Dataset for disaster risk data"""

    def __init__(self, features: np.ndarray, labels: np.ndarray):
        self.features = torch.tensor(features, dtype=torch.float32)
        self.labels = torch.tensor(labels, dtype=torch.long)

    def __len__(self) -> int:
        return len(self.features)

    def __getitem__(self, idx: int) -> Tuple[torch.Tensor, torch.Tensor]:
        return self.features[idx], self.labels[idx]


class ModelTrainer:
    """Handles model training, validation, and saving"""

    def __init__(
        self,
        input_size: int = 20,
        hidden_sizes: list = None,
        learning_rate: float = 0.001,
        batch_size: int = 32,
        num_epochs: int = 100,
        device: Optional[str] = None
    ):
        self.input_size = input_size
        self.hidden_sizes = hidden_sizes or [128, 64, 32]
        self.learning_rate = learning_rate
        self.batch_size = batch_size
        self.num_epochs = num_epochs

        if device:
            self.device = torch.device(device)
        else:
            self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        logger.info(f"Using device: {self.device}")

        self.model = DisasterRiskModel(
            input_size=input_size,
            hidden_sizes=hidden_sizes
        ).to(self.device)

        self.criterion = nn.CrossEntropyLoss()
        self.optimizer = optim.Adam(self.model.parameters(), lr=learning_rate)
        self.scheduler = optim.lr_scheduler.ReduceLROnPlateau(
            self.optimizer,
            mode='min',
            factor=0.5,
            patience=10,
            verbose=True
        )

        self.training_history = {
            "train_loss": [],
            "val_loss": [],
            "train_accuracy": [],
            "val_accuracy": []
        }

    def generate_synthetic_data(
        self,
        num_samples: int = 10000,
        test_split: float = 0.2
    ) -> Tuple[DataLoader, DataLoader]:
        """Generate synthetic training data for demonstration"""

        logger.info(f"Generating {num_samples} synthetic samples...")

        features = []
        labels = []

        for _ in range(num_samples):
            # Generate random features
            sample_features = np.random.rand(self.input_size)

            # Simulate risk level based on certain feature combinations
            # Higher values in certain features indicate higher risk
            risk_indicators = [
                sample_features[4],  # seismic_activity
                sample_features[5],  # flood_risk
                sample_features[6],  # fire_danger_index
                sample_features[2],  # wind_speed
                sample_features[3],  # precipitation
            ]

            avg_risk = np.mean(risk_indicators)

            # Add some noise and complexity
            population_factor = sample_features[7]  # population_density
            vulnerability_factor = sample_features[10]  # vulnerable_population_ratio

            risk_score = (
                avg_risk * 0.6 +
                population_factor * 0.2 +
                vulnerability_factor * 0.2
            )

            # Map continuous risk score to discrete categories
            if risk_score < 0.2:
                label = 0  # Very Low
            elif risk_score < 0.4:
                label = 1  # Low
            elif risk_score < 0.6:
                label = 2  # Moderate
            elif risk_score < 0.8:
                label = 3  # High
            else:
                label = 4  # Critical

            features.append(sample_features)
            labels.append(label)

        features = np.array(features)
        labels = np.array(labels)

        # Create dataset
        dataset = DisasterDataset(features, labels)

        # Split into train and validation
        val_size = int(len(dataset) * test_split)
        train_size = len(dataset) - val_size

        train_dataset, val_dataset = random_split(
            dataset,
            [train_size, val_size],
            generator=torch.Generator().manual_seed(42)
        )

        # Create data loaders
        train_loader = DataLoader(
            train_dataset,
            batch_size=self.batch_size,
            shuffle=True,
            num_workers=0
        )

        val_loader = DataLoader(
            val_dataset,
            batch_size=self.batch_size,
            shuffle=False,
            num_workers=0
        )

        logger.info(f"Training samples: {train_size}, Validation samples: {val_size}")
        logger.info(f"Label distribution: {np.bincount(labels)}")

        return train_loader, val_loader

    def load_data_from_csv(
        self,
        csv_path: str,
        test_split: float = 0.2
    ) -> Tuple[DataLoader, DataLoader]:
        """Load and prepare data from CSV file"""

        logger.info(f"Loading data from {csv_path}...")

        df = pd.read_csv(csv_path)

        # Assume last column is label, rest are features
        features = df.iloc[:, :-1].values
        labels = df.iloc[:, -1].values

        # Normalize features
        features = (features - features.mean(axis=0)) / (features.std(axis=0) + 1e-8)

        # Create dataset
        dataset = DisasterDataset(features, labels)

        # Split into train and validation
        val_size = int(len(dataset) * test_split)
        train_size = len(dataset) - val_size

        train_dataset, val_dataset = random_split(
            dataset,
            [train_size, val_size],
            generator=torch.Generator().manual_seed(42)
        )

        # Create data loaders
        train_loader = DataLoader(
            train_dataset,
            batch_size=self.batch_size,
            shuffle=True,
            num_workers=0
        )

        val_loader = DataLoader(
            val_dataset,
            batch_size=self.batch_size,
            shuffle=False,
            num_workers=0
        )

        logger.info(f"Training samples: {train_size}, Validation samples: {val_size}")

        return train_loader, val_loader

    def train_epoch(self, train_loader: DataLoader) -> Tuple[float, float]:
        """Train for one epoch"""

        self.model.train()
        total_loss = 0.0
        correct = 0
        total = 0

        for batch_idx, (features, labels) in enumerate(train_loader):
            features = features.to(self.device)
            labels = labels.to(self.device)

            # Forward pass
            self.optimizer.zero_grad()
            outputs = self.model(features)
            loss = self.criterion(outputs, labels)

            # Backward pass
            loss.backward()
            self.optimizer.step()

            # Statistics
            total_loss += loss.item()
            _, predicted = outputs.max(1)
            total += labels.size(0)
            correct += predicted.eq(labels).sum().item()

        avg_loss = total_loss / len(train_loader)
        accuracy = 100.0 * correct / total

        return avg_loss, accuracy

    def validate(self, val_loader: DataLoader) -> Tuple[float, float]:
        """Validate the model"""

        self.model.eval()
        total_loss = 0.0
        correct = 0
        total = 0

        with torch.no_grad():
            for features, labels in val_loader:
                features = features.to(self.device)
                labels = labels.to(self.device)

                outputs = self.model(features)
                loss = self.criterion(outputs, labels)

                total_loss += loss.item()
                _, predicted = outputs.max(1)
                total += labels.size(0)
                correct += predicted.eq(labels).sum().item()

        avg_loss = total_loss / len(val_loader)
        accuracy = 100.0 * correct / total

        return avg_loss, accuracy

    def train(
        self,
        train_loader: DataLoader,
        val_loader: DataLoader,
        early_stopping_patience: int = 20
    ):
        """Complete training loop with early stopping"""

        logger.info("Starting training...")
        logger.info(f"Epochs: {self.num_epochs}, Batch size: {self.batch_size}")

        best_val_loss = float('inf')
        patience_counter = 0

        for epoch in range(self.num_epochs):
            # Train
            train_loss, train_acc = self.train_epoch(train_loader)

            # Validate
            val_loss, val_acc = self.validate(val_loader)

            # Update learning rate
            self.scheduler.step(val_loss)

            # Save history
            self.training_history["train_loss"].append(train_loss)
            self.training_history["val_loss"].append(val_loss)
            self.training_history["train_accuracy"].append(train_acc)
            self.training_history["val_accuracy"].append(val_acc)

            # Logging
            if (epoch + 1) % 10 == 0:
                logger.info(
                    f"Epoch [{epoch+1}/{self.num_epochs}] "
                    f"Train Loss: {train_loss:.4f}, Train Acc: {train_acc:.2f}% | "
                    f"Val Loss: {val_loss:.4f}, Val Acc: {val_acc:.2f}%"
                )

            # Early stopping
            if val_loss < best_val_loss:
                best_val_loss = val_loss
                patience_counter = 0
            else:
                patience_counter += 1

            if patience_counter >= early_stopping_patience:
                logger.info(f"Early stopping triggered at epoch {epoch+1}")
                break

        logger.info("Training completed!")
        logger.info(f"Best validation loss: {best_val_loss:.4f}")

    def save_model(self, output_path: str):
        """Save trained model and training history"""

        output_dir = Path(output_path).parent
        output_dir.mkdir(parents=True, exist_ok=True)

        # Save model state
        torch.save(self.model.state_dict(), output_path)
        logger.info(f"Model saved to {output_path}")

        # Save training history
        history_path = output_path.replace(".pth", "_history.json")
        with open(history_path, 'w') as f:
            json.dump(self.training_history, f, indent=2)
        logger.info(f"Training history saved to {history_path}")

        # Save model metadata
        metadata = {
            "timestamp": datetime.utcnow().isoformat(),
            "input_size": self.input_size,
            "hidden_sizes": self.hidden_sizes,
            "learning_rate": self.learning_rate,
            "batch_size": self.batch_size,
            "num_epochs": self.num_epochs,
            "device": str(self.device),
            "final_train_loss": self.training_history["train_loss"][-1],
            "final_val_loss": self.training_history["val_loss"][-1],
            "final_train_accuracy": self.training_history["train_accuracy"][-1],
            "final_val_accuracy": self.training_history["val_accuracy"][-1]
        }

        metadata_path = output_path.replace(".pth", "_metadata.json")
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f, indent=2)
        logger.info(f"Model metadata saved to {metadata_path}")

    def load_model(self, model_path: str):
        """Load a trained model"""

        self.model.load_state_dict(torch.load(model_path, map_location=self.device))
        self.model.eval()
        logger.info(f"Model loaded from {model_path}")


def main():
    """Main training script"""

    # Configuration
    config = {
        "input_size": 20,
        "hidden_sizes": [128, 64, 32],
        "learning_rate": 0.001,
        "batch_size": 32,
        "num_epochs": 100,
        "num_samples": 10000,
        "test_split": 0.2,
        "output_path": "models/disaster_risk_model.pth"
    }

    # Initialize trainer
    trainer = ModelTrainer(
        input_size=config["input_size"],
        hidden_sizes=config["hidden_sizes"],
        learning_rate=config["learning_rate"],
        batch_size=config["batch_size"],
        num_epochs=config["num_epochs"]
    )

    # Generate or load data
    # Option 1: Generate synthetic data
    train_loader, val_loader = trainer.generate_synthetic_data(
        num_samples=config["num_samples"],
        test_split=config["test_split"]
    )

    # Option 2: Load from CSV (uncomment to use)
    # train_loader, val_loader = trainer.load_data_from_csv(
    #     csv_path="data/disaster_data.csv",
    #     test_split=config["test_split"]
    # )

    # Train model
    trainer.train(train_loader, val_loader, early_stopping_patience=20)

    # Save model
    trainer.save_model(config["output_path"])

    logger.info("Training pipeline completed successfully!")


if __name__ == "__main__":
    main()
